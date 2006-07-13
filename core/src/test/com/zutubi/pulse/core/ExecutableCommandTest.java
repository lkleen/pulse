package com.zutubi.pulse.core;

import com.zutubi.pulse.core.model.*;
import com.zutubi.pulse.test.PulseTestCase;
import com.zutubi.pulse.util.FileSystemUtils;
import com.zutubi.pulse.util.IOUtils;
import com.zutubi.pulse.util.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 
 *
 */
public class ExecutableCommandTest extends PulseTestCase
{
    private File baseDirectory;
    private File outputDirectory;

    public void setUp() throws Exception
    {
        super.setUp();
        baseDirectory = FileSystemUtils.createTempDirectory(ExecutableCommandTest.class.getName(), ".base");
        outputDirectory = FileSystemUtils.createTempDirectory(ExecutableCommandTest.class.getName(), ".out");
    }

    public void tearDown() throws Exception
    {
        FileSystemUtils.removeDirectory(outputDirectory);
        FileSystemUtils.removeDirectory(baseDirectory);
        super.tearDown();
    }

    public void testExecuteSuccessExpected() throws Exception
    {
        ExecutableCommand command = new ExecutableCommand();
        command.setExe("echo");
        command.setArgs("hello world");
        CommandResult result = new CommandResult("success");
        execute(command, result);
        assertEquals(result.getState(), ResultState.SUCCESS);
    }

    public void testExecuteFailureExpected() throws Exception
    {
        ExecutableCommand command = new ExecutableCommand();
        command.setExe("dir");
        command.setArgs("w");
        CommandResult result = new CommandResult("failure");
        execute(command, result);
        assertEquals(result.getState(), ResultState.FAILURE);
    }

    public void testExecuteSuccessExpectedNoArg() throws Exception
    {
        ExecutableCommand command = new ExecutableCommand();
        command.setExe("netstat");
        CommandResult result = new CommandResult("no arg");
        execute(command, result);
        assertEquals(result.getState(), ResultState.SUCCESS);
    }

    public void testExecuteExceptionExpected() throws Exception
    {
        ExecutableCommand command = new ExecutableCommand();
        command.setExe("unknown");
        command.setArgs("command");
        try
        {
            execute(command, new CommandResult("exception"));
            assertTrue(false);
        }
        catch (BuildException e)
        {
            // noop            
        }
    }

    public void testPostProcess() throws FileLoadException
    {
        ExecutableCommand command = new ExecutableCommand();
        command.setExe("echo");
        command.setArgs("error: badness");

        ProcessArtifact processArtifact = command.createProcess();
        RegexPostProcessor processor = new RegexPostProcessor();
        RegexPattern regex = new RegexPattern();
        regex.setCategory("error");
        regex.setExpression("error:.*");
        processor.addRegexPattern(regex);
        processArtifact.setProcessor(processor);

        CommandResult cmdResult = new CommandResult("processed");
        execute(command, cmdResult);
        assertEquals(ResultState.FAILURE, cmdResult.getState());

        StoredArtifact artifact = cmdResult.getArtifact(ExecutableCommand.OUTPUT_NAME);
        List<Feature> features = artifact.getFeatures(Feature.Level.ERROR);
        assertEquals(1, features.size());
        Feature feature = features.get(0);
        assertEquals(Feature.Level.ERROR, feature.getLevel());
        assertEquals("error: badness", feature.getSummary());
    }

    public void testWorkingDir() throws IOException
    {
        File dir = new File(baseDirectory, "nested");
        File file;

        assertTrue(dir.mkdir());

        if(SystemUtils.isWindows())
        {
            file = new File(dir, "list.bat");
            FileSystemUtils.createFile(file, "dir");
        }
        else
        {
            file = new File(dir, "./list.sh");
            FileSystemUtils.createFile(file, "#! /bin/sh\nls");
            FileSystemUtils.setPermissions(file, 777);
        }


        ExecutableCommand command = new ExecutableCommand();
        command.setWorkingDir(new File("nested"));
        command.setExe(file.getPath());

        CommandResult result = new CommandResult("work");
        execute(command, result);
        assertTrue(result.succeeded());
    }

    public void testExtraPathInScope() throws IOException
    {
        File data = getTestDataFile("core", "scope", "bin");
        Scope scope = new Scope();
        scope.add(new ResourceProperty("mypath", data.getAbsolutePath(), false, true));

        ExecutableCommand command = new ExecutableCommand();
        command.setExe("custom");
        command.setScope(scope);

        CommandResult result = new CommandResult("work");
        execute(command, result);
        assertTrue(result.succeeded());
    }

    public void testEnvironmentVariableFromScope() throws IOException
    {
        File data = getTestDataFile("core", "scope", "bin");
        Scope scope = new Scope();
        scope.add(new ResourceProperty("mypath", data.getAbsolutePath(), false, true));
        scope.add(new ResourceProperty("TESTVAR", "test variable value", true, false));

        ExecutableCommand command = new ExecutableCommand();
        command.setExe("custom");
        command.setScope(scope);

        CommandResult result = new CommandResult("work");
        execute(command, result);
        assertTrue(result.succeeded());
        String output = getOutput();
        assertTrue(output.contains("test variable value"));
    }

    private String getOutput() throws IOException
    {
        return IOUtils.fileToString(new File(outputDirectory, "command output/output.txt"));
    }

    private void execute(ExecutableCommand command, CommandResult result)
    {
        CommandContext context = new CommandContext(new SimpleRecipePaths(baseDirectory, null), outputDirectory);
        command.execute(0, context, result);
    }
}
