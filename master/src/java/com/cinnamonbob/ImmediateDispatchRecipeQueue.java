package com.cinnamonbob;

import com.cinnamonbob.core.BuildException;
import com.cinnamonbob.events.Event;
import com.cinnamonbob.events.EventListener;
import com.cinnamonbob.events.EventManager;
import com.cinnamonbob.events.SlaveAvailableEvent;
import com.cinnamonbob.events.build.RecipeDispatchedEvent;
import com.cinnamonbob.model.Slave;
import com.cinnamonbob.model.SlaveManager;
import com.cinnamonbob.util.logging.Logger;

import java.net.MalformedURLException;
import java.util.LinkedList;
import java.util.List;

/**
 * The RecipeQueue takes in RecipeRequests and allocates them as efficiently
 * as possible to build hosts.  It is assumed all requests in the queue may
 * be handled in parallel.
 */
public class ImmediateDispatchRecipeQueue implements RecipeQueue, EventListener
{
    private static final Logger LOG = Logger.getLogger(ImmediateDispatchRecipeQueue.class);

    private List<BuildService> buildServices;
    private SlaveManager slaveManager;
    private SlaveProxyFactory slaveProxyFactory;
    private EventManager eventManager;

    public ImmediateDispatchRecipeQueue()
    {
        buildServices = new LinkedList<BuildService>();
    }

    public void init()
    {
        buildServices.add(new MasterBuildService());

        for (Slave slave : slaveManager.getAll())
        {
            SlaveBuildService slaveService = createSlaveService(slave);
            if (slaveService != null)
            {
                buildServices.add(slaveService);
            }
        }

        eventManager.register(this);
    }

    private SlaveBuildService createSlaveService(Slave slave)
    {
        try
        {
            return new SlaveBuildService(slave, slaveProxyFactory.createProxy(slave));
        }
        catch (MalformedURLException e)
        {
            LOG.severe("Error creating build service for slave '" + slave.getName() + "': " + e.getMessage(), e);
        }

        return null;
    }

    public void enqueue(RecipeDispatchRequest request)
    {
        // This assumes we can dispatch now, which is unlikely to be a
        // practical dispatch algorithm!
        for (BuildService service : buildServices)
        {
            if (request.getHostRequirements().fulfilledBy(service))
            {
                service.build(request.getRequest());
                eventManager.publish(new RecipeDispatchedEvent(this, request.getRequest().getId(), service));
                return;
            }
        }

        throw new BuildException("No build service found to handle request.");
    }

    public List<RecipeDispatchRequest> takeSnapshot()
    {
        // Never actually queue anything!
        return new LinkedList<RecipeDispatchRequest>();
    }

    public void handleEvent(Event evt)
    {
        SlaveAvailableEvent event = (SlaveAvailableEvent) evt;
        SlaveBuildService newService = createSlaveService(event.getSlave());

        if (newService != null)
        {
            for (BuildService service : buildServices)
            {
                if (service.equals(newService))
                {
                    return;
                }
            }
        }

        buildServices.add(newService);
    }

    public Class[] getHandledEvents()
    {
        return new Class[]{SlaveAvailableEvent.class};
    }

    public void setSlaveProxyFactory(SlaveProxyFactory slaveProxyFactory)
    {
        this.slaveProxyFactory = slaveProxyFactory;
    }

    public void setEventManager(EventManager eventManager)
    {
        this.eventManager = eventManager;
    }

    public void setSlaveManager(SlaveManager slaveManager)
    {
        this.slaveManager = slaveManager;
    }

}
