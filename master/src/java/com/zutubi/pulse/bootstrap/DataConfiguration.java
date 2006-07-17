package com.zutubi.pulse.bootstrap;

import com.zutubi.pulse.bootstrap.conf.Config;
import com.zutubi.pulse.bootstrap.conf.FileConfig;
import com.zutubi.pulse.bootstrap.conf.PropertiesConfig;
import com.zutubi.pulse.util.logging.Logger;

import java.io.File;

/**
 * <class-comment/>
 */
public class DataConfiguration implements DataResolver
{
    private static final Logger LOG = Logger.getLogger(DataConfiguration.class);

    private static final String FILENAME = "pulse-init.properties";

    private Config systemProps = null;
    private Config initProps = null;

    private SystemPaths paths = null;

    /**
     * The data property.
     */
    private static final String PULSE_DATA = "pulse.data";

    public Data getData()
    {
        File dir = getDataDirectory();
        if (dir != null)
        {
            return new Data(dir);
        }
        return null;
    }

    public void setDataDirectory(File f)
    {
        Config init = getInitProps();
        init.setProperty(PULSE_DATA, f.getAbsolutePath());
    }

    public File getDataDirectory()
    {
        Config sys = getSystemProps();
        if (sys.hasProperty(PULSE_DATA))
        {
            return new File(sys.getProperty(PULSE_DATA));
        }
        Config init = getInitProps();
        if (init.hasProperty(PULSE_DATA))
        {
            return new File(init.getProperty(PULSE_DATA));
        }
        return null;
    }

    private Config getSystemProps()
    {
        if (systemProps == null)
        {
            systemProps = new PropertiesConfig(System.getProperties());
        }
        return systemProps;
    }

    private Config getInitProps()
    {
        if (initProps == null)
        {
            initProps = new FileConfig(new File(paths.getConfigRoot(), FILENAME));
        }
        return initProps;
    }

    public void setConfigurationManager(MasterConfigurationManager configurationManager)
    {
        setSystemPaths(configurationManager.getSystemPaths());
    }

    /**
     * Required resource.
     *
     * @param paths
     */
    public void setSystemPaths(SystemPaths paths)
    {
        this.paths = paths;
    }
}
