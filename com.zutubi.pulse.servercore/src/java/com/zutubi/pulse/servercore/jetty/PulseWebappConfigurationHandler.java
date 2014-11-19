package com.zutubi.pulse.servercore.jetty;

import com.opensymphony.util.TextUtils;
import com.zutubi.pulse.servercore.bootstrap.ConfigurationManager;
import com.zutubi.pulse.servercore.bootstrap.SystemConfiguration;
import com.zutubi.util.Constants;
import com.zutubi.util.StringUtils;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;

/**
 * The configuration handler for the main Pulse web application.
 */
public class PulseWebappConfigurationHandler implements ServerConfigurationHandler, ContextConfigurationHandler
{
    private static final String[] LOGGING_IGNORE_PATHS = new String[]{"/images/*.*", "*.css", "*.js", "*.ico", "*.gif"};
    private static final String PROPERTY_IDLE_TIMEOUT = "pulse.jetty.idle.timeout";
    private static final int DEFAULT_IDLE_TIMEOUT = (int) (60 * Constants.SECOND);
    private static final String PROPERTY_MIN_THREADS = "pulse.jetty.min.threads";
    private static final String PROPERTY_MAX_THREADS = "pulse.jetty.max.threads";

    private ConfigurationManager configurationManager;

    /**
     * The temporary directory available to the web applications.
     */
    private File tmpDir;

    /**
     * The directory into which log files are to be written.
     */
    private File logDir;

    public void configure(Server server) throws IOException
    {
        SystemConfiguration config = configurationManager.getSystemConfig();
        String bindAddress = config.getBindAddress();
        if (bindAddress.contains(","))
        {
            for (String host : StringUtils.split(bindAddress, ',', true))
            {
                host = host.trim();
                addConnector(server, config, host);
            }
        }
        else
        {
            addConnector(server, config, bindAddress);
        }

        QueuedThreadPool threadPool = (QueuedThreadPool) server.getThreadPool();
        threadPool.setMinThreads(Integer.getInteger(PROPERTY_MIN_THREADS, threadPool.getMinThreads()));
        threadPool.setMaxThreads(Integer.getInteger(PROPERTY_MAX_THREADS, threadPool.getMaxThreads()));
    }

    private void addConnector(Server server, SystemConfiguration config, String host) throws UnknownHostException
    {
        ServerConnector connector;
        if (config.isSslEnabled())
        {
            SslContextFactory sslContextFactory = new SslContextFactory();
            if (config.getSslKeystore() != null)
            {
                sslContextFactory.setKeyStorePath(config.getSslKeystore());
            }
            sslContextFactory.setKeyStorePassword(config.getSslPassword());
            sslContextFactory.setKeyManagerPassword(config.getSslKeyPassword());

            connector = new ServerConnector(server, new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()));
        }
        else
        {
            connector = new ServerConnector(server, new HttpConnectionFactory(new HttpConfiguration()));
        }

        connector.setHost(host);
        connector.setPort(config.getServerPort());
        connector.setIdleTimeout(getIdleTimeout());
        server.addConnector(connector);
    }

    private int getIdleTimeout()
    {
        String timeoutValue = System.getProperty(PROPERTY_IDLE_TIMEOUT);
        if (TextUtils.stringSet(timeoutValue))
        {
            try
            {
                return (int) (Integer.parseInt(timeoutValue) * Constants.SECOND);
            }
            catch (NumberFormatException e)
            {
                return DEFAULT_IDLE_TIMEOUT;
            }
        }
        else
        {
            return DEFAULT_IDLE_TIMEOUT;
        }
    }

    public void configure(ContextHandler context) throws IOException
    {
        SystemConfiguration config = configurationManager.getSystemConfig();
        context.setContextPath(config.getContextPath());

        if (!tmpDir.exists() && !tmpDir.mkdirs())
        {
            throw new IOException("Failed to create " + tmpDir.getCanonicalPath());
        }

        context.setAttribute("javax.servlet.context.tempdir", tmpDir);

        HandlerCollection handlers = new HandlerCollection();

        WebAppContext webApp = new WebAppContext(configurationManager.getSystemPaths().getContentRoot().getAbsolutePath(), null);
        webApp.setDefaultsDescriptor(null);
        handlers.addHandler(webApp);

        if (isRequestLoggingEnabled())
        {
            NCSARequestLog requestLog = new NCSARequestLog();
            requestLog.setAppend(false);
            requestLog.setExtended(isRequestLoggingExtended());
            requestLog.setIgnorePaths(getRequestLoggingIgnorePaths());
            requestLog.setRetainDays(getDaysLogsRetained());
            requestLog.setFilename(new File(logDir, "yyyy_mm_dd.request.log").getAbsolutePath());

            RequestLogHandler logHandler = new RequestLogHandler();
            logHandler.setRequestLog(requestLog);
            handlers.addHandler(logHandler);
        }

        context.setHandler(handlers);
    }

    private String[] getRequestLoggingIgnorePaths()
    {
        return LOGGING_IGNORE_PATHS;
    }

    private Integer getDaysLogsRetained()
    {
        return Integer.getInteger("pulse.request.logging.retain.days", 30);
    }

    private boolean isRequestLoggingExtended()
    {
        return Boolean.getBoolean("pulse.extended.request.logging");
    }

    private boolean isRequestLoggingEnabled()
    {
        return Boolean.getBoolean("pulse.enable.request.logging");
    }

    public void setConfigurationManager(ConfigurationManager configurationManager)
    {
        this.configurationManager = configurationManager;
    }

    public void setLogDir(File logDir)
    {
        this.logDir = logDir;
    }

    public void setTmpDir(File tmpDir)
    {
        this.tmpDir = tmpDir;
    }
}
