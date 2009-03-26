package com.zutubi.pulse.core.commands.api;

import com.zutubi.pulse.core.PulseExecutionContext;
import com.zutubi.pulse.core.SimpleRecipePaths;
import static com.zutubi.pulse.core.engine.api.BuildProperties.*;
import com.zutubi.pulse.core.engine.api.ExecutionContext;
import com.zutubi.pulse.core.postprocessors.api.PostProcessorConfiguration;
import com.zutubi.pulse.core.test.api.PulseTestCase;
import com.zutubi.util.FileSystemUtils;
import com.zutubi.util.io.IOUtils;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Helper base class for implementing test cases for commands.
 */
public abstract class CommandTestCase extends PulseTestCase
{
    protected File tempDir;
    protected File baseDir;
    protected File outputDir;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        tempDir = FileSystemUtils.createTempDir(getClass().getName() + "." + getName(), ".tmp");
        baseDir = new File(tempDir, "base");
        assertTrue(baseDir.mkdir());
        outputDir = new File(tempDir, "output");
        assertTrue(outputDir.mkdir());
    }

    @Override
    public void tearDown() throws Exception
    {
        removeDirectory(tempDir);
        super.tearDown();
    }

    /**
     * Creates a minimal execution context to use as part of the command
     * context.
     * 
     * @return a new, minimal execution context
     */
    protected ExecutionContext createExecutionContext()
    {
        PulseExecutionContext context = new PulseExecutionContext();
        context.addValue(NAMESPACE_INTERNAL, PROPERTY_OUTPUT_DIR, outputDir);
        context.addValue(NAMESPACE_INTERNAL, PROPERTY_RECIPE_PATHS, new SimpleRecipePaths(baseDir, outputDir));
        context.setWorkingDir(baseDir);
        return context;
    }

    /**
     * Runs the given command with a minimal execution context, returning a
     * test context recording the results.
     *
     * @param command the command to run
     * @return a test context that can be inspected to determine the results
     *         of running the command
     * @throws Exception on any error
     */
    protected TestCommandContext runCommand(Command command) throws Exception
    {
        return runCommand(command, createExecutionContext());
    }

    /**
     * Runs the given command with the given execution context, returning a
     * test context recording the results.
     *
     * @param command the command to run
     * @param context the context in which to execute the command
     * @return a test context that can be inspected to determine the results
     *         of running the command
     */
    protected TestCommandContext runCommand(Command command, ExecutionContext context)
    {
        TestCommandContext commandContext = new TestCommandContext(context);
        try
        {
            command.execute(commandContext);
            return commandContext;
        }
        finally
        {
            commandContext.complete();
        }
    }

    /**
     * Gets a file object pointing at the file within the given output with the
     * given path.
     *
     * @param name the name of the registered output the file should have been
     *             captured in
     * @param path the path of the file under the output directory
     * @return a file object pointing to the given file in the given output
     */
    protected File getFile(String name, String path)
    {
        File dir = new File(outputDir, name);
        return new File(dir, path);
    }

    /**
     * Asserts a file exists (was captured during execution) in the given
     * output with the given path.
     *
     * @param name the name of the registered output the file should have been
     *             captured in
     * @param path the path of the file under the output directory to test for
     */
    protected void assertFile(String name, String path)
    {
        File file = getFile(name, path);
        assertTrue("File '" + file.getPath() + "' does not exist", file.exists());   
    }

    /**
     * Gets the contents of a file captured in the given output with the given
     * path.
     *
     * @param name the name of the registered output the file should have been
     *             captured in
     * @param path the path of the file under the output directory
     * @return the entire contents of the file
     * @throws java.io.IOException if there is an error reading the file
     */
    protected String getFileContent(String name, String path) throws IOException
    {
        return IOUtils.fileToString(getFile(name, path));
    }

    /**
     * Asserts that the file captured in the given output with the given path
     * contains all of the given strings.
     *
     * @param name     the name of the registered output the file should have
     *                 been captured in
     * @param path     the path of the file under the output directory
     * @param contents the strings to test for in the file
     * @throws IOException if there is an error reading the file
     */
    protected void assertFileContains(String name, String path, String... contents) throws IOException
    {
        assertFileContains(name, path, true, contents);
    }

    /**
     * Asserts that the file captured in the given output with the given path
     * contains all of the given strings, possibly case-insensitively.
     *
     * @param name          the name of the registered output the file should
     *                      have been captured in
     * @param path          the path of the file under the output directory
     * @param caseSensitive if true, the search is case-insensitive
     * @param contents the strings to test for in the file
     * @throws IOException if there is an error reading the file
     */
    protected void assertFileContains(String name, String path, boolean caseSensitive, String... contents) throws IOException
    {
        String output = getFileContent(name, path);
        if (!caseSensitive)
        {
            output = output.toLowerCase();
        }

        for (String content: contents)
        {
            if (!caseSensitive)
            {
                content = content.toLowerCase();
            }

            assertThat(output, containsString(content));
        }
    }

    /**
     * Asserts the given output string contains each of the given content
     * strings.
     *
     * @param output   the output string to search within
     * @param contents the strings to search for
     */
    protected void assertOutputContains(String output, String... contents)
    {
        assertOutputContains(output, true, contents);
    }

    /**
     * Asserts the given output string contains each of the given content
     * strings, possibly case-insensitively.
     *
     * @param output        the output string to search within
     * @param caseSensitive if true, the search is case-insensitive
     * @param contents      the strings to search for
     */
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

    /**
     * Asserts that an output matching the given expected output has been
     * registered with the given context.  Used to check that execution of a
     * command registered an expected output.
     *
     * @param expectedOutput the expected output, including post-processors
     *                       that should be registered against it
     * @param context        context from the command execution
     */
    protected void assertOutputRegistered(TestCommandContext.Output expectedOutput, TestCommandContext context)
    {
        TestCommandContext.Output gotOutput = context.getOutputs().get(expectedOutput.getName());
        assertNotNull("Expected output '" + expectedOutput.getName() + "' not registered");
        assertEquals(expectedOutput.getIndex(), gotOutput.getIndex());

        List<PostProcessorConfiguration> expectedProcessors = expectedOutput.getAppliedProcessors();
        List<PostProcessorConfiguration> gotProcessors = gotOutput.getAppliedProcessors();
        assertEquals(expectedProcessors.size(), gotProcessors.size());
        for (int i = 0; i < expectedProcessors.size(); i++)
        {
            assertSame(expectedProcessors.get(i), gotProcessors.get(i));   
        }
    }
}
