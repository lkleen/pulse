package com.zutubi.pulse.master.scm;

import com.zutubi.pulse.core.scm.api.ScmContext;
import com.zutubi.pulse.core.scm.api.ScmException;
import com.zutubi.pulse.master.tove.config.project.ProjectConfiguration;

/**
 * Interface for the scm context factory.  The context factory is used to maintain the
 * scm context instances.  These instances hold persistent data between scm invocations.
 */
public interface ScmContextFactory
{
    ScmContext createContext(ProjectConfiguration projectConfiguration) throws ScmException;
}