package com.zutubi.pulse.master.scm;

import com.zutubi.pulse.core.scm.ScmContextImpl;
import com.zutubi.pulse.core.scm.api.ScmException;
import com.zutubi.pulse.core.scm.api.ScmContext;
import com.zutubi.pulse.core.scm.config.ScmConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of the scm context factory interface.
 */
public class DefaultScmContextFactory implements ScmContextFactory
{
    private File projectsDir;

    private final Map<Long, ScmContextImpl> contexts = new HashMap<Long, ScmContextImpl>();

    public ScmContext createContext(long id, ScmConfiguration config) throws ScmException
    {
        try
        {
            synchronized(contexts)
            {
                if (!contexts.containsKey(config.getHandle()))
                {
                    ScmContextImpl context = new ScmContextImpl();
                    File projectDir = new File(projectsDir, String.valueOf(id));
                    File workingDir = new File(projectDir, "scm");
                    if (!workingDir.isDirectory() && !workingDir.mkdirs())
                    {
                        throw new IOException("Failed to create persistent working directory '" + workingDir.getCanonicalPath() + "'");
                    }
                    context.setPersistentWorkingDir(workingDir);
                    contexts.put(config.getHandle(), context);
                }
            }
            return contexts.get(config.getHandle());
        }
        catch (IOException e)
        {
            throw new ScmException("IO Failure creating scm context. " + e.getMessage(), e);
        }
    }

    public void setProjectsDir(File projectsDir)
    {
        this.projectsDir = projectsDir;
    }


}
