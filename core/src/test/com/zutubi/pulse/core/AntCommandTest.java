package com.zutubi.pulse.core;

import com.zutubi.pulse.core.model.CommandResult;

import java.io.IOException;

/**
 */
public class AntCommandTest extends CommandTestBase
{
    public void testBasicDefault() throws IOException
    {
        copyBuildFile("basic");
        AntCommand command = new AntCommand();
        successRun(command, "build target");
    }

    public void testBasicTargets() throws IOException
    {
        copyBuildFile("basic");
        AntCommand command = new AntCommand();
        command.setTargets("build test");
        successRun(command, "build target", "test target");
    }

    public void testDoubleSpaceTargets() throws IOException
    {
        copyBuildFile("basic");
        AntCommand command = new AntCommand();
        command.setTargets("build  test");
        successRun(command, "build target", "test target");
    }

    public void testEnvironment() throws IOException
    {
        copyBuildFile("basic");
        AntCommand command = new AntCommand();
        command.setTargets("environment");
        ExecutableCommand.Environment env = command.createEnvironment();
        env.setName("TEST_ENV_VAR");
        env.setValue("test variable value");
        successRun(command, "test variable value");
    }

    public void testExplicitBuildfile() throws IOException
    {
        copyBuildFile("basic", "custom.xml");
        AntCommand command = new AntCommand();
        command.setBuildFile("custom.xml");
        successRun(command, "build target");
    }

    public void testExplicitArguments() throws IOException
    {
        copyBuildFile("basic", "custom.xml");
        AntCommand command = new AntCommand();
        command.setBuildFile("custom.xml");
        command.setTargets("build");
        ExecutableCommand.Arg arg = command.createArg();
        arg.setText("test");
        successRun(command, "build target", "test target");
    }

    public void testRunNoBuildFile() throws IOException
    {
        AntCommand command = new AntCommand();
        failedRun(command, "Buildfile: build.xml does not exist!");
    }

    public void testRunNonExistantBuildFile() throws IOException
    {
        AntCommand command = new AntCommand();
        command.setBuildFile("nope.xml");
        failedRun(command, "Buildfile: nope.xml does not exist!");
    }

    protected String getBuildFileName()
    {
        return "build.xml";
    }

    protected String getBuildFileExt()
    {
        return "xml";
    }

    protected void checkOutput(CommandResult commandResult, String ...contents) throws IOException
    {
        super.checkOutput(commandResult, contents);
    }
}