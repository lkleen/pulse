package com.zutubi.pulse.slave;

import com.zutubi.pulse.bootstrap.ComponentContext;
import com.zutubi.pulse.bootstrap.Startup;
import com.zutubi.pulse.bootstrap.StartupException;
import com.zutubi.pulse.bootstrap.SystemConfiguration;
import com.zutubi.pulse.config.ConfigSupport;
import com.zutubi.pulse.config.FileConfig;
import com.zutubi.pulse.core.ObjectFactory;
import com.zutubi.pulse.core.Stoppable;
import com.zutubi.pulse.util.logging.Logger;
import org.mortbay.http.SocketListener;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.WebApplicationContext;
import org.mortbay.util.InetAddrPort;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 */
public class SlaveStartupManager implements Startup, Stoppable
{
    private static final Logger LOG = Logger.getLogger(SlaveStartupManager.class);

    private List<String> systemContexts;
    private List<String> startupRunnables = new LinkedList<String>();
    private ObjectFactory objectFactory;
    private Server jettyServer;
    private SlaveConfigurationManager configurationManager;
    private long startTime;

    public void init() throws StartupException
    {
        ComponentContext.addClassPathContextDefinitions(systemContexts.toArray(new String[systemContexts.size()]));

        SystemConfiguration config = configurationManager.getSystemConfig();
        File configRoot = configurationManager.getSystemPaths().getConfigRoot();
        File startupConfigFile = new File(configRoot, "runtime.properties");
        ConfigSupport startupConfig = new ConfigSupport(new FileConfig(startupConfigFile));
        startupConfig.setProperty(SystemConfiguration.CONTEXT_PATH, config.getContextPath());
        startupConfig.setInteger(SystemConfiguration.WEBAPP_PORT, config.getServerPort());

        runStartupRunnables();

        jettyServer = new Server();
        int port = config.getServerPort();
        SocketListener listener = new SocketListener(new InetAddrPort(port));
        jettyServer.addListener(listener);
        try
        {
            WebApplicationContext context = jettyServer.addWebApplication(config.getContextPath(), configurationManager.getSystemPaths().getContentRoot().getAbsolutePath());
            context.setDefaultsDescriptor(null);
            jettyServer.start();
            startTime = System.currentTimeMillis();
            System.out.println("The agent is now listening on port: " + port);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public long getUptime()
    {
        return System.currentTimeMillis() - startTime;
    }

    private void runStartupRunnables()
    {
        for (String name : startupRunnables)
        {
            try
            {
                Runnable instance = objectFactory.buildBean(name);
                instance.run();
            }
            catch (Exception e)
            {
                LOG.warning("Failed to run startup task " + name + ". Reason: " + e.getMessage(), e);
            }
        }
    }

    public void setSystemContexts(List<String> systemContexts)
    {
        this.systemContexts = systemContexts;
    }

    public void setStartupRunnables(List<String> startupRunnables)
    {
        this.startupRunnables = startupRunnables;
    }

    public void setObjectFactory(ObjectFactory objectFactory)
    {
        this.objectFactory = objectFactory;
    }

    public void setConfigurationManager(SlaveConfigurationManager configurationManager)
    {
        this.configurationManager = configurationManager;
    }

    public void stop(boolean force)
    {
        try
        {
            jettyServer.stop(true);
        }
        catch (InterruptedException e)
        {
            // Ignore.
        }
    }
}
