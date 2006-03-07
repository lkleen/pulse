package com.cinnamonbob;

import com.cinnamonbob.bootstrap.ConfigurationManager;
import com.cinnamonbob.core.RecipePaths;

import java.io.File;

/**
 * The server recipe paths:
 * <p/>
 * system/recipes/xyz/work
 * /output
 * <p/>
 * where xyz is the recipe identifier.
 */
public class ServerRecipePaths implements RecipePaths
{
    private long id;
    private ConfigurationManager configurationManager;

    public ServerRecipePaths(long id, ConfigurationManager configurationManager)
    {
        this.id = id;
        this.configurationManager = configurationManager;
    }

    private File getRecipesRoot()
    {
        return new File(configurationManager.getUserPaths().getHome(), "recipes");
    }

    public File getRecipeRoot()
    {
        return new File(getRecipesRoot(), Long.toString(id));
    }

    public File getBaseDir()
    {
        return new File(getRecipeRoot(), "work");
    }

    public File getOutputDir()
    {
        return new File(getRecipeRoot(), "output");
    }

    public File getBaseZip()
    {
        return new File(getBaseDir().getAbsolutePath() + ".zip");
    }

    public File getOutputZip()
    {
        return new File(getOutputDir().getAbsolutePath() + ".zip");
    }
}
