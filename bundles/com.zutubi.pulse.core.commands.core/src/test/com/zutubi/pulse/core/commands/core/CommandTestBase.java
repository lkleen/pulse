package com.zutubi.pulse.core.commands.core;

import com.zutubi.pulse.core.*;
import com.zutubi.pulse.core.engine.api.BuildException;
import static com.zutubi.pulse.core.engine.api.BuildProperties.*;
import com.zutubi.pulse.core.model.CommandResult;
import com.zutubi.pulse.core.model.PersistentFeature;
import com.zutubi.pulse.core.model.StoredArtifact;
import com.zutubi.pulse.core.model.StoredFileArtifact;
import com.zutubi.pulse.core.postprocessors.api.Feature;
import com.zutubi.pulse.core.test.api.PulseTestCase;
import com.zutubi.util.FileSystemUtils;
import com.zutubi.util.TextUtils;
import com.zutubi.util.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

public abstract class CommandTestBase extends PulseTestCase
{
    protected File baseDir;
    protected File outputDir;

    public CommandTestBase()
    {
    }

    public CommandTestBase(String name)
    {
        super(name);
    }

    public void setUp() throws IOException
    {
        baseDir = FileSystemUtils.createTempDir(getClass().getName(), ".base");
        outputDir = FileSystemUtils.createTempDir(getClass().getName(), ".out");
    }

    public void tearDown() throws IOException
    {
        removeDirectory(baseDir);
        removeDirectory(outputDir);
    }

    protected CommandResult runCommand(Command command) throws Exception
    {
        return runCommand(command, new PulseExecutionContext());
    }

    /**
     * Simple framework for method for running a command within the context of a recipe.
     *
     * @param command to be executed.
     * @param context for the commands execution, if one exists.
     * @return the command result instance generated by the execution of this command.
     */
    protected CommandResult runCommand(Command command, PulseExecutionContext context)
    {
        context.addValue(NAMESPACE_INTERNAL, PROPERTY_RECIPE_PATHS, new SimpleRecipePaths(baseDir, outputDir));
        context.setWorkingDir(baseDir);

        // For convenience, supply a name if there is not one already.
        if (!TextUtils.stringSet(command.getName()))
        {
            command.setName(getName());
        }

        CommandResult result = new CommandResult(command.getName());
        result.commence();
        File commandOutput = new File(outputDir, Recipe.getCommandDirName(0, result));
        if (!commandOutput.mkdirs())
        {
            throw new BuildException("Could not create command output directory '" + commandOutput.getAbsolutePath() + "'");
        }

        final String LABEL_EXECUTE = "execute";
        context.setLabel(LABEL_EXECUTE);
        context.push();
        context.addString(NAMESPACE_INTERNAL, PROPERTY_OUTPUT_DIR, commandOutput.getAbsolutePath());
        try
        {
            RecipeProcessor.executeAndProcess(context, result, command);
        }
        finally
        {
            result.complete();
            context.popTo(LABEL_EXECUTE);
        }
        return result;
    }

    protected void checkContents(File outputFile, String... contents) throws IOException
    {
        checkContents(outputFile, true, contents);
    }

    protected void checkArtifact(CommandResult result, StoredArtifact artifact, String... contents) throws IOException
    {
        assertNotNull(artifact);
        File expectedFile = getCommandArtifact(result, artifact);
        checkContents(expectedFile, true, contents);
    }

    protected void checkContents(File outputFile, boolean caseSensitive, String... contents) throws IOException
    {
        FileInputStream is = null;
        try
        {
            is = new FileInputStream(outputFile);
            String output = IOUtils.inputStreamToString(is);
            assertOutputContains(output, caseSensitive, contents);
        }
        finally
        {
            IOUtils.close(is);
        }
    }

    protected void assertOutputContains(String output, String... contents)
    {
        assertOutputContains(output, true, contents);
    }

    protected void assertOutputContains(String output, boolean caseSensitive, String... contents)
    {
        if (!caseSensitive)
        {
            output = output.toLowerCase();
        }
        for (String content : contents)
        {
            if (!caseSensitive)
            {
                content = content.toLowerCase();
            }
            if (!output.contains(content))
            {
                fail("Output '" + output + "' does not contain '" + content + "'");
            }
        }
    }

    protected File getCommandArtifact(CommandResult result, StoredFileArtifact fileArtifact)
    {
        String commandDirName = String.format("00000000-%s", result.getCommandName());
        return new File(outputDir, FileSystemUtils.composeFilename(commandDirName, fileArtifact.getPath()));
    }

    protected File getCommandArtifact(CommandResult result, StoredArtifact artifact)
    {
        String commandDirName = String.format("00000000-%s", result.getCommandName());
        return new File(outputDir, FileSystemUtils.composeFilename(commandDirName, artifact.getFile().getPath()));
    }

    protected StoredFileArtifact getOutputArtifact(CommandResult result)
    {
        return result.getFileArtifact(ExecutableCommand.OUTPUT_ARTIFACT_NAME + "/" + ExecutableCommand.OUTPUT_FILENAME);
    }

    protected void assertErrorsMatch(StoredFileArtifact artifact, String... summaryRegexes)
    {
        assertFeatures(artifact, Feature.Level.ERROR, summaryRegexes);
    }

    protected void assertWarningsMatch(StoredFileArtifact artifact, String... summaryRegexes)
    {
        assertFeatures(artifact, Feature.Level.WARNING, summaryRegexes);
    }

    protected void assertFeatures(StoredFileArtifact artifact, Feature.Level level, String... summaryRegexes)
    {
        List<PersistentFeature> features = artifact.getFeatures(level);
        assertEquals(summaryRegexes.length, features.size());
        for(int i = 0; i < summaryRegexes.length; i++)
        {
            String summary = features.get(i).getSummary();
            assertTrue("Summary '" + summary + "' does not match regex '" + summaryRegexes[i], summary.matches(summaryRegexes[i]));
        }
    }
}
