package com.zutubi.pulse.servercore;

import com.zutubi.pulse.core.BootstrapperSupport;
import com.zutubi.pulse.core.BuildRevision;
import com.zutubi.pulse.core.PulseExecutionContext;
import com.zutubi.pulse.core.RecipePaths;
import com.zutubi.pulse.core.commands.api.CommandContext;
import com.zutubi.pulse.core.engine.api.BuildException;
import static com.zutubi.pulse.core.engine.api.BuildProperties.*;
import com.zutubi.util.FileSystemUtils;
import static com.zutubi.util.FileSystemUtils.*;
import com.zutubi.util.StringUtils;

import java.io.File;
import java.io.IOException;

/**
 * The Project Repository Bootstrapper checks out a project into the project recipes
 * persistent working directory and then runs an update when necessary. The results
 * are then copied into the builds base directory if it differs from the checkout
 * directory.
 */
public class ProjectRepositoryBootstrapper extends BootstrapperSupport
{
    private final String projectName;
    private final BuildRevision revision;
    private ScmBootstrapper childBootstrapper;

    public ProjectRepositoryBootstrapper(String projectName, BuildRevision revision)
    {
        this.projectName = projectName;
        this.revision = revision;
    }

    public void doBootstrap(final CommandContext commandContext) throws BuildException
    {
        PulseExecutionContext context = (PulseExecutionContext) commandContext.getExecutionContext();
        final RecipePaths paths = context.getValue(NAMESPACE_INTERNAL, PROPERTY_RECIPE_PATHS, RecipePaths.class);

        File checkoutDir = paths.getCheckoutDir();
        childBootstrapper = selectBootstrapper(checkoutDir);

        String ignoreDirs;
        context.push();
        File baseDir = paths.getBaseDir();
        try
        {
            context.addValue(NAMESPACE_INTERNAL, PROPERTY_RECIPE_PATHS, paths);
            context.setWorkingDir(checkoutDir);
            childBootstrapper.bootstrap(commandContext);
            ignoreDirs = context.getString(NAMESPACE_INTERNAL, PROPERTY_IGNORE_DIRS);
        }
        finally
        {
            context.setWorkingDir(baseDir);
            context.pop();
        }

        // If the builds base directory differs from the checkout directory, then we need to copy over
        // to the base, this implies a CLEAN_UPDATE checkout scheme.
        if(!baseDir.equals(checkoutDir))
        {
            writeFeedback("Copying source from " + getNormalisedAbsolutePath(checkoutDir) +
                    " to " + getNormalisedAbsolutePath(baseDir) + ".  This may take some time.");
            try
            {
                if (StringUtils.stringSet(ignoreDirs))
                {
                    writeFeedback("Excluding directories with names: '" + ignoreDirs + "'.");
                    String[] excludes = StringUtils.split(ignoreDirs, ',');
                    ensureEmptyDirectory(baseDir);
                    recursiveCopy(baseDir, checkoutDir, excludes);
                }
                else
                {
                    copy(baseDir, checkoutDir);
                }
            }
            catch (IOException e)
            {
                throw new BuildException(e);
            }
        }
    }

    public void terminate()
    {
        super.terminate();
        if(childBootstrapper != null)
        {
            childBootstrapper.terminate();
        }
    }

    private ScmBootstrapper selectBootstrapper(final File localDir)
    {
        if (!localDir.exists() && !localDir.mkdirs())
        {
            throw new BuildException("Failed to initialise local scm directory: " + localDir.getAbsolutePath());
        }

        if (FileSystemUtils.list(localDir).length == 0)
        {
            return new CheckoutBootstrapper(projectName, revision);
        }
        else // else we can update.
        {
            return new UpdateBootstrapper(projectName, revision);
        }
    }
}
