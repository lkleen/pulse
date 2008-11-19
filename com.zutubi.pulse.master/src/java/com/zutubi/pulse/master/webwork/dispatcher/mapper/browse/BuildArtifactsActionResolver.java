package com.zutubi.pulse.master.webwork.dispatcher.mapper.browse;

import com.zutubi.pulse.master.webwork.dispatcher.mapper.ActionResolver;
import com.zutubi.pulse.master.webwork.dispatcher.mapper.ActionResolverSupport;

/**
 * Resolves to the artifacts view for a build.
 */
public class BuildArtifactsActionResolver extends ActionResolverSupport
{
    public BuildArtifactsActionResolver()
    {
        super("viewBuildArtifacts");
    }

    public ActionResolver getChild(String name)
    {
        return new StageArtifactsActionResolver(name, false);
    }
}
