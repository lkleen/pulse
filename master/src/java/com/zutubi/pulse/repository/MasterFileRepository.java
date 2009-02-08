package com.zutubi.pulse.repository;

import com.zutubi.pulse.MasterBuildPaths;
import com.zutubi.pulse.bootstrap.MasterConfigurationManager;

import java.io.File;

/**
 */
public class MasterFileRepository implements FileRepository
{
    private MasterBuildPaths buildPaths;

    public MasterFileRepository(MasterConfigurationManager configurationManager)
    {
        buildPaths = new MasterBuildPaths(configurationManager);
    }

    public File getPatchFile(long userId, long number)
    {
        return buildPaths.getUserPatchFile(userId, number);
    }

}