package com.zutubi.pulse.core.commands.api;

import com.zutubi.pulse.core.engine.api.BuildException;
import com.zutubi.pulse.core.util.PulseZipUtils;
import com.zutubi.util.FileSystemUtils;
import com.zutubi.util.StringUtils;
import org.apache.tools.ant.DirectoryScanner;

import java.io.File;
import java.io.IOException;

/**
 * An artifact that captures a set of files under a base directory.
 *
 * @see DirectoryArtifactConfiguration
 */
public class DirectoryArtifact extends FileSystemArtifactSupport
{
    /**
     * Constructor that stores the configuration.
     *
     * @param config configuration for this atifact
     * @see #getConfig() 
     */
    public DirectoryArtifact(DirectoryArtifactConfiguration config)
    {
        super(config);
    }

    protected void captureFiles(File toDir, CommandContext context)
    {
        DirectoryArtifactConfiguration config = (DirectoryArtifactConfiguration) getConfig();
        String base = config.getBase();
        File baseDir;
        if (!StringUtils.stringSet(base))
        {
            baseDir = context.getExecutionContext().getWorkingDir();
        }
        else
        {
            baseDir = new File(base);
            if (!isAbsolute(baseDir))
            {
                baseDir = new File(context.getExecutionContext().getWorkingDir(), baseDir.getPath());
            }
        }

        if (!baseDir.exists())
        {
            if(config.isFailIfNotPresent() && ! context.getResultState().isBroken())
            {
                throw new BuildException("Capturing artifact '" + config.getName() + "': base directory '" + baseDir.getAbsolutePath() + "' does not exist");
            }
            else
            {
                // Don't attempt to capture.
                return;
            }
        }

        if (!baseDir.isDirectory())
        {
            throw new BuildException("Directory artifact '" + config.getName() + "': base '" + baseDir.getAbsolutePath() + "' is not a directory");
        }

        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(baseDir);
        if (!config.getInclusions().isEmpty())
        {
            scanner.setIncludes(config.getInclusions().toArray(new String[config.getInclusions().size()]));
        }

        if (!config.getExclusions().isEmpty())
        {
            scanner.setExcludes(config.getExclusions().toArray(new String[config.getExclusions().size()]));
        }

        scanner.setFollowSymlinks(config.isFollowSymlinks());
        scanner.scan();

        context.setArtifactIndex(config.getName(), config.getIndex());
        for (String file : scanner.getIncludedFiles())
        {
            captureFile(new File(toDir, file), new File(baseDir, file), context);
        }
        
        if (config.isCaptureAsZip())
        {
            zipCapturedFiles(config, toDir);
        }
    }

    private void zipCapturedFiles(DirectoryArtifactConfiguration config, File toDir)
    {
        File zipFile = new File(toDir.getParent(), config.getName() + ".zip");
        try
        {
            PulseZipUtils.createZip(zipFile, toDir, null);
        }
        catch (IOException e)
        {
            throw new BuildException("Directory artifact '" + config.getName() + "': unable to zip captured files: " + e.getMessage(), e);
        }

        try
        {
            FileSystemUtils.rmdir(toDir);
        }
        catch (IOException e)
        {
            throw new BuildException("Directory artifact '" + config.getName() + "': unable to cleanup files after zipping: " + e.getMessage(), e);
        }
        
        if (!toDir.mkdir())
        {
            throw new BuildException("Directory artifact '" + config.getName() + "': unable to recreate destination directory '" + toDir.getAbsolutePath() + "' after zipping");            
        }

        File finalZip = new File(toDir, zipFile.getName());
        if (!zipFile.renameTo(finalZip))
        {
            throw new BuildException("Directory artifact '" + config.getName() + "': unable to rename zip to '" + finalZip + "'");
        }
    }
}
