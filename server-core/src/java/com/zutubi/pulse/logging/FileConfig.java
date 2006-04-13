/********************************************************************************
 @COPYRIGHT@
 ********************************************************************************/
package com.zutubi.pulse.logging;

import com.zutubi.pulse.bootstrap.ConfigurationManager;
import com.zutubi.pulse.bootstrap.ComponentContext;

import java.util.logging.*;
import java.io.File;
import java.io.IOException;

/**
 * <class-comment/>
 */
public class FileConfig
{
    private static final String FILE_NAME = "pulse%u.%g.log";

    private ConfigurationManager configManager;
    private static final int APPROX_FILESIZE_LIMIT = 1000000;
    private static final int FILE_ROLL_COUNT = 20;
    private static final boolean APPEND = true;

    public FileConfig() throws IOException
    {
        // hack the autowiring
        ComponentContext.autowire(this);

        File logRoot = configManager.getSystemPaths().getLogRoot();
        if (!logRoot.exists() && !logRoot.mkdirs())
        {
            throw new IOException();
        }
        if (logRoot.exists() && !logRoot.isDirectory())
        {
            throw new IOException();
        }

        String pattern = logRoot.getCanonicalPath() + File.separator + FILE_NAME;

        FileHandler fileHandler = new FileHandler(pattern, APPROX_FILESIZE_LIMIT, FILE_ROLL_COUNT, APPEND);

        // leave it up to the log levels to decide what ends up in the log file.
        fileHandler.setLevel(Level.ALL);
        fileHandler.setFormatter(new SimpleFormatter());

        Logger rootLogger = Logger.getLogger("");
        rootLogger.addHandler(fileHandler);
    }

    public void setConfigurationManager(ConfigurationManager configurationManager)
    {
        this.configManager = configurationManager;
    }
}
