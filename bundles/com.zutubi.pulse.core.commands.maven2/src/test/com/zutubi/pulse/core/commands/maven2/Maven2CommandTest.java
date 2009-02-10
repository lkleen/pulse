package com.zutubi.pulse.core.commands.maven2;

import com.zutubi.pulse.core.PulseExecutionContext;
import com.zutubi.pulse.core.commands.api.TestCommandContext;
import com.zutubi.pulse.core.commands.core.ExecutableCommandTestCase;
import com.zutubi.util.FileSystemUtils;

import java.io.IOException;

public class Maven2CommandTest extends ExecutableCommandTestCase
{
    public void testBasic() throws Exception
    {
        prepareBaseDir("basic");

        Maven2CommandConfiguration command = new Maven2CommandConfiguration();
        command.setGoals("compile");
        successRun(command, "[compiler:compile]", "BUILD SUCCESSFUL");
    }

    public void testExtractVersion() throws Exception
    {
        prepareBaseDir("basic");

        Maven2CommandConfiguration command = new Maven2CommandConfiguration();
        command.setGoals("compile");
        PulseExecutionContext context = (PulseExecutionContext) createExecutionContext();
        runCommand(new Maven2Command(command), context);
        assertEquals("1.0-SNAPSHOT", context.getVersion());
    }

    public void testNoTarget() throws Exception
    {
        prepareBaseDir("basic");

        Maven2CommandConfiguration command = new Maven2CommandConfiguration();
        failedRun(command, "BUILD FAILURE", "You must specify at least one goal");
    }

    public void testMultiGoal() throws Exception
    {
        prepareBaseDir("basic");

        Maven2CommandConfiguration command = new Maven2CommandConfiguration();
        command.setGoals("compile test");
        successRun(command, "BUILD SUCCESSFUL", "Running com.zutubi.maven2.test.AppTest",
                "task-segment: [compile, test]", "[compiler:compile]", "[compiler:testCompile]", "[surefire:test]",
                "Tests run: 1, Failures: 0, Errors: 0");
    }

    public void testNoPOM() throws Exception
    {
        prepareBaseDir("nopom");

        Maven2CommandConfiguration command = new Maven2CommandConfiguration();
        command.setGoals("compile");
        failedRun(command, "BUILD ERROR", "Cannot execute mojo: resources", "It requires a project with an existing pom.xml");
    }

    public void testNonDefaultPOM() throws Exception
    {
        prepareBaseDir("nondefaultpom");

        Maven2CommandConfiguration command = new Maven2CommandConfiguration();
        command.setGoals("compile");
        command.setArgs("-f blah/pom.xml");
        successRun(command, "[compiler:compile]", "BUILD SUCCESSFUL");
    }

    public void testCompilerError() throws Exception
    {
        prepareBaseDir("compilererror");

        Maven2CommandConfiguration command = new Maven2CommandConfiguration();
        command.setGoals("compile");
        failedRun(command, "Compilation failure", "BUILD FAILURE", "task-segment: [compile]");
    }

    public void testTestFailure() throws Exception
    {
        prepareBaseDir("testfailure");

        Maven2CommandConfiguration command = new Maven2CommandConfiguration();
        command.setGoals("test");
        failedRun(command, "task-segment: [test]", "There are test failures.");
    }

    // FIXME loader
//    public void testAppliesProcessor() throws Exception
//    {
//        prepareBaseDir("testfailure");
//
//        Maven2CommandConfiguration command = new Maven2CommandConfiguration();
//        command.setGoals("test");
//        TestCommandContext commandContext = failedRun(command);
//        List<PostProcessorConfiguration> appliedProcessors = commandContext.getOutputs().get("command output").getAppliedProcessors();
//        assertEquals(1, appliedProcessors.size());
//        assertTrue(appliedProcessors.get(0) instanceof Maven2PostProcessorConfiguration);
//    }

    private void prepareBaseDir(String name) throws IOException
    {
        FileSystemUtils.rmdir(baseDir);
        assertTrue(baseDir.mkdir());

        unzipInput(name, baseDir);
    }

    private TestCommandContext successRun(Maven2CommandConfiguration configuration, String... content) throws Exception
    {
        return successRun(new Maven2Command(configuration), content);
    }

    private TestCommandContext failedRun(Maven2CommandConfiguration configuration, String... content) throws Exception
    {
        return failedRun(new Maven2Command(configuration), content);
    }
}