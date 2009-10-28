package com.zutubi.pulse.master.agent;

import com.zutubi.events.Event;
import com.zutubi.events.EventListener;
import com.zutubi.events.EventManager;
import com.zutubi.pulse.Version;
import com.zutubi.pulse.master.events.HostPingEvent;
import com.zutubi.pulse.master.scheduling.Scheduler;
import com.zutubi.pulse.master.scheduling.SchedulingException;
import com.zutubi.pulse.master.scheduling.SimpleTrigger;
import com.zutubi.pulse.master.scheduling.Trigger;
import com.zutubi.pulse.master.scheduling.tasks.PingSlaves;
import com.zutubi.pulse.master.tove.config.admin.AgentPingConfiguration;
import com.zutubi.pulse.servercore.agent.PingStatus;
import com.zutubi.pulse.servercore.services.HostStatus;
import com.zutubi.pulse.servercore.util.background.BackgroundServiceSupport;
import com.zutubi.tove.config.TypeAdapter;
import com.zutubi.tove.config.TypeListener;
import com.zutubi.tove.events.ConfigurationEventSystemStartedEvent;
import com.zutubi.util.Constants;
import com.zutubi.util.logging.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages the task of pinging hosts and sending out the results as events.
 * Duplicate ping requests are filtered, and ping timeouts are managed (ping
 * results are sent by the timeout even if the pinging thread is still
 * awaiting a response from a remote agent).
 */
public class HostPingService extends BackgroundServiceSupport implements EventListener
{
    private static final Logger LOG = Logger.getLogger(HostPingService.class);

    private static final String PING_NAME = "ping";
    private static final String PING_GROUP = "services";

    private static final int DEFAULT_POOL_SIZE = 24;

    private final int masterBuildNumber = Version.getVersion().getBuildNumberAsInt();
    private AgentPingConfiguration configuration;
    private Lock inProgressLock = new ReentrantLock();
    private Set<Long> inProgress = new HashSet<Long>();
    private EventManager eventManager;
    private MasterLocationProvider masterLocationProvider;
    private Scheduler scheduler;

    public HostPingService()
    {
        // Establish a default pool size as the config is not yet available
        super("Agent Ping", DEFAULT_POOL_SIZE);
    }

    /**
     * Requests that the given host is pinged.  When the ping completes an
     * {@link com.zutubi.pulse.master.events.HostPingEvent} is raised.
     *
     * If there is currently a ping in progress for the agent, this request
     * is ignored.  However, it is guaranteed that a ping event will be
     * raised after this method is called.
     *
     * @param host        the host to ping
     * @param hostService service for communicating with the host
     * @return true if the request was submitted, false if it was filtered
     *         due to a pending request for the same agent
     */
    public boolean requestPing(Host host, HostService hostService)
    {
        inProgressLock.lock();
        try
        {
            // Ignore duplicate requests.  If there is ping result pending
            // for this agent, don't request another one.
            if (isPingInProgress(host))
            {
                return false;
            }
            else
            {
                pingStarted(host);
                enqueueRequest(host, hostService);
                return true;
            }
        }
        finally
        {
            inProgressLock.unlock();
        }
    }

    /**
     * Check to see if a ping is currently in progress for the specified host.
     *
     * @param host  the host for which we are checking for a ping.
     * @return  true if a ping is currently in progress to the specified host, false otherwise.
     */
    public boolean isPingInProgress(Host host)
    {
        return inProgress.contains(host.getId());
    }

    private void pingStarted(Host host)
    {
        inProgress.add(host.getId());
    }

    private void pingCompleted(Host host)
    {
        inProgressLock.lock();
        try
        {
            inProgress.remove(host.getId());
        }
        finally
        {
            inProgressLock.unlock();
        }
    }

    private void enqueueRequest(final Host host, HostService hostService)
    {
        // Directly submit the ping to the pool for execution.  Note that
        // this thread may be stuck until a network timeout.
        ExecutorService threadPool = getExecutorService();
        final Future<HostStatus> future = threadPool.submit(new HostPing(host, hostService, masterBuildNumber, masterLocationProvider.getMasterUrl()));

        // Run a second thread to wait for up to the agent ping timeout for
        // the result of the ping.  This way we can send out the ping event
        // after at most the agent timeout period, even when the original
        // thread is still waiting on the network.
        threadPool.execute(new Runnable()
        {
            public void run()
            {
                HostStatus status;
                try
                {
                    status = future.get(configuration.getPingTimeout(), TimeUnit.SECONDS);
                }
                catch (TimeoutException e)
                {
                    if (configuration.isTimeoutLoggingEnabled())
                    {
                        LOG.warning("Timed out pinging host '" + host.getLocation() + "'", e);
                    }
                    
                    status = new HostStatus(PingStatus.OFFLINE, "Host ping timed out");
                }
                catch (Exception e)
                {
                    LOG.debug(e);

                    String message = "Unexpected error pinging host '" + host.getLocation() + "': " + e.getMessage();
                    LOG.warning(message);
                    status = new HostStatus(PingStatus.OFFLINE, message);
                }

                pingCompleted(host);
                eventManager.publish(new HostPingEvent(this, host, status));
            }
        });
    }

    void refreshSettings(AgentPingConfiguration agentPingConfig)
    {
        configuration = agentPingConfig;
        int poolSize = agentPingConfig.getMaxConcurrent() * 2;
        setPoolSize(poolSize);

        try
        {
            Trigger trigger = scheduler.getTrigger(PING_NAME, PING_GROUP);
            if (trigger != null)
            {
                scheduler.unschedule(trigger);
            }

            trigger = new SimpleTrigger(PING_NAME, PING_GROUP, agentPingConfig.getPingInterval() * Constants.SECOND);
            trigger.setTaskClass(PingSlaves.class);
            scheduler.schedule(trigger);
        }
        catch (SchedulingException e)
        {
            LOG.severe(e);
        }

    }

    public void handleEvent(Event event)
    {
        ConfigurationEventSystemStartedEvent eventSystemStartedEvent = (ConfigurationEventSystemStartedEvent) event;
        TypeListener typeListener = new TypeAdapter<AgentPingConfiguration>(AgentPingConfiguration.class)
        {
            @Override
            public void postSave(AgentPingConfiguration instance, boolean nested)
            {
                refreshSettings(instance);
            }
        };

        typeListener.register(eventSystemStartedEvent.getConfigurationProvider(), false);
        init();
        refreshSettings(eventSystemStartedEvent.getConfigurationProvider().get(AgentPingConfiguration.class));
    }

    public Class[] getHandledEvents()
    {
        return new Class[]{ ConfigurationEventSystemStartedEvent.class };
    }
    
    public void setMasterLocationProvider(MasterLocationProvider masterLocationProvider)
    {
        this.masterLocationProvider = masterLocationProvider;
    }

    public void setEventManager(EventManager eventManager)
    {
        this.eventManager = eventManager;
        eventManager.register(this);
    }

    public void setScheduler(Scheduler scheduler)
    {
        this.scheduler = scheduler;
    }
}
