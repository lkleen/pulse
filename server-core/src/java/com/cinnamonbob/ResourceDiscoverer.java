package com.cinnamonbob;

import com.cinnamonbob.core.ResourceRepository;
import com.cinnamonbob.core.model.Property;
import com.cinnamonbob.core.model.Resource;
import com.cinnamonbob.core.util.SystemUtils;
import com.cinnamonbob.model.persistence.ResourceDao;

import java.io.File;

/**
 */
public class ResourceDiscoverer implements Runnable
{
    private ResourceRepository resourceRepository;
    private ResourceDao resourceDao;

    public void run()
    {
        discoverAnt();
        discoverMake();
        discoverJava();
    }

    private void discoverAnt()
    {
        if (!resourceRepository.hasResource("ant"))
        {
            String home = System.getenv("ANT_HOME");
            if (home != null)
            {
                Resource antResource = new Resource("ant");
                antResource.addProperty(new Property("ant.home", home));
                File antBin;

                if (SystemUtils.isWindows())
                {
                    antBin = new File(home, "bin/ant.bat");
                }
                else
                {
                    antBin = new File(home, "bin/ant");
                }

                if (antBin.isFile())
                {
                    antResource.addProperty(new Property("ant.bin", antBin.getAbsolutePath()));
                }

                File antLib = new File(home, "lib");
                if (antLib.isDirectory())
                {
                    antResource.addProperty(new Property("ant.lib.dir", antLib.getAbsolutePath()));
                }

                resourceDao.save(antResource);
            }
        }
    }

    private void discoverMake()
    {
        if (!resourceRepository.hasResource("make"))
        {
            File makeBin = SystemUtils.findInPath("make");
            if (makeBin != null)
            {
                Resource makeResource = new Resource("make");
                makeResource.addProperty(new Property("make.bin", makeBin.getAbsolutePath()));
                resourceDao.save(makeResource);
            }
        }
    }

    private void discoverJava()
    {
        if (resourceRepository.hasResource("java"))
        {
            return;
        }

        //TODO: look for java on the path.

        // look for JAVA_HOME in the environment.
        String home = System.getenv("JAVA_HOME");

        Resource javaResource = new Resource("java");
        javaResource.addProperty(new Property("java.home", home));

        File javaBin;
        if (SystemUtils.isWindows())
        {
            javaBin = new File(home, "bin/java.exe");
        }
        else
        {
            javaBin = new File(home, "bin/java");
        }

        if (javaBin.isFile())
        {
            javaResource.addProperty(new Property("java.bin", javaBin.getAbsolutePath()));
            resourceDao.save(javaResource);
        }
    }

    public void setResourceRepository(ResourceRepository resourceRepository)
    {
        this.resourceRepository = resourceRepository;
    }

    public void setResourceDao(ResourceDao resourceDao)
    {
        this.resourceDao = resourceDao;
    }
}
