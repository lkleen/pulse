package com.zutubi.pulse.master;

import com.zutubi.pulse.core.BuildRevision;
import com.zutubi.pulse.core.engine.api.BuildProperties;
import com.zutubi.pulse.core.engine.api.ExecutionContext;
import com.zutubi.pulse.core.model.CommandResult;
import com.zutubi.pulse.core.model.RecipeResult;
import com.zutubi.pulse.core.model.TestResultSummary;
import com.zutubi.pulse.core.scm.config.api.CheckoutScheme;
import com.zutubi.pulse.master.agent.MasterLocationProvider;
import com.zutubi.pulse.master.bootstrap.MasterConfigurationManager;
import com.zutubi.pulse.master.model.*;
import com.zutubi.pulse.master.tove.config.project.BuildStageConfiguration;
import com.zutubi.pulse.master.tove.config.project.ProjectConfiguration;
import com.zutubi.pulse.master.tove.config.project.ResourcePropertyConfiguration;

import java.io.File;
import java.util.Date;

/**
 * A utility extension of the build properties class with static members for
 * populating the build context with data that only makes sense on the
 * master.
 */
public class MasterBuildProperties extends BuildProperties
{
    public static void addProjectProperties(ExecutionContext context, ProjectConfiguration projectConfiguration)
    {
        for(ResourcePropertyConfiguration property: projectConfiguration.getProperties().values())
        {
            context.add(property.asResourceProperty());
        }
    }

    public static void addAllBuildProperties(ExecutionContext context, BuildResult result, MasterLocationProvider masterLocationProvider, MasterConfigurationManager configurationManager)
    {
        addBuildProperties(context, result, result.getProject(), result.getAbsoluteOutputDir(configurationManager.getDataDirectory()), masterLocationProvider.getMasterUrl());
        if(result.getRevision() != null)
        {
            context.addString(NAMESPACE_INTERNAL, PROPERTY_BUILD_REVISION, result.getRevision().getRevisionString());
        }
        context.addString(NAMESPACE_INTERNAL, PROPERTY_BUILD_TIMESTAMP, TIMESTAMP_FORMAT.format(result.getStamps().getStartTime()));
        context.addString(NAMESPACE_INTERNAL, PROPERTY_BUILD_TIMESTAMP_MILLIS, Long.toString(result.getStamps().getStartTime()));
        addProjectProperties(context, result.getProject().getConfig());
        addCompletedBuildProperties(context, result, configurationManager);
    }

    public static void addBuildProperties(ExecutionContext context, BuildResult buildResult, Project project, File buildOutputDir, String masterUrl)
    {
        ProjectConfiguration projectConfig = project.getConfig();
        context.addString(NAMESPACE_INTERNAL, PROPERTY_BUILD_NUMBER, Long.toString(buildResult.getNumber()));
        context.addString(NAMESPACE_INTERNAL, PROPERTY_PROJECT, projectConfig.getName());
        if (buildOutputDir != null)
        {
            context.addString(NAMESPACE_INTERNAL, PROPERTY_BUILD_DIRECTORY, buildOutputDir.getAbsolutePath());
        }

        BuildReason buildReason = buildResult.getReason();
        context.addString(NAMESPACE_INTERNAL, PROPERTY_BUILD_REASON, buildReason.getSummary());
        if(buildReason instanceof TriggerBuildReason)
        {
            context.addString(NAMESPACE_INTERNAL, PROPERTY_BUILD_TRIGGER, ((TriggerBuildReason)buildReason).getTriggerName());
        }

        context.addString(NAMESPACE_INTERNAL, PROPERTY_PERSONAL_BUILD, Boolean.toString(buildResult.isPersonal()));

        context.addString(NAMESPACE_INTERNAL, PROPERTY_MASTER_URL, masterUrl);
        context.addString(NAMESPACE_INTERNAL, PROPERTY_BUILD_COUNT, Integer.toString(project.getBuildCount()));
        context.addString(NAMESPACE_INTERNAL, PROPERTY_SUCCESS_COUNT, Integer.toString(project.getSuccessCount()));

        CheckoutScheme checkoutScheme = projectConfig.getScm().getCheckoutScheme();
        context.addString(NAMESPACE_INTERNAL, PROPERTY_INCREMENTAL_BOOTSTRAP, Boolean.toString(!buildResult.isPersonal() && checkoutScheme != CheckoutScheme.CLEAN_CHECKOUT));
        context.addString(NAMESPACE_INTERNAL, PROPERTY_INCREMENTAL_BUILD, Boolean.toString(!buildResult.isPersonal() && checkoutScheme == CheckoutScheme.INCREMENTAL_UPDATE));

        context.addString(NAMESPACE_INTERNAL, PROPERTY_COMPRESS_ARTIFACTS, "true");
        context.addString(NAMESPACE_INTERNAL, PROPERTY_COMPRESS_WORKING_DIR, Boolean.toString(projectConfig.getOptions().getRetainWorkingCopy()));
    }

    public static void addRevisionProperties(ExecutionContext context, BuildRevision buildRevision)
    {
        context.addString(NAMESPACE_INTERNAL, PROPERTY_BUILD_REVISION, buildRevision.getRevision().getRevisionString());
        context.addString(NAMESPACE_INTERNAL, PROPERTY_BUILD_TIMESTAMP, TIMESTAMP_FORMAT.format(new Date(buildRevision.getTimestamp())));
        context.addString(NAMESPACE_INTERNAL, PROPERTY_BUILD_TIMESTAMP_MILLIS, Long.toString(buildRevision.getTimestamp()));
    }

    public static void addCompletedBuildProperties(ExecutionContext context, BuildResult result, MasterConfigurationManager configurationManager)
    {
        context.addString(NAMESPACE_INTERNAL, PROPERTY_STATUS, result.getState().getString());

        TestResultSummary tests = result.getTestSummary();
        String testSummary;
        if(tests.getTotal() > 0)
        {
            if(tests.allPassed())
            {
                testSummary = "all " + tests.getTotal() + " tests passed";
            }
            else
            {
                testSummary = Integer.toString(tests.getBroken()) + " of " + tests.getTotal() + " tests broken";
            }
        }
        else
        {
            testSummary = "no tests";
        }

        context.addString(NAMESPACE_INTERNAL, PROPERTY_TEST_SUMMARY, testSummary);
        for(RecipeResultNode node: result.getRoot().getChildren())
        {
            addStageProperties(context, result, node, configurationManager, true);
        }
    }

    public static void addStageProperties(ExecutionContext context, BuildResult result, RecipeResultNode node, MasterConfigurationManager configurationManager, boolean includeName)
    {
        String name = node.getStageName();
        String prefix = "stage.";

        RecipeResult recipeResult = node.getResult();
        if(includeName)
        {
            prefix += name + ".";
        }
        else
        {
            BuildStageConfiguration stage = result.getProject().getConfig().getStage(node.getStageName());
            if (stage != null)
            {
                for(ResourcePropertyConfiguration property: stage.getProperties().values())
                {
                    context.add(property.asResourceProperty());
                }
            }

            context.addString(NAMESPACE_INTERNAL, PROPERTY_RECIPE, recipeResult.getRecipeNameSafe());
            context.addString(NAMESPACE_INTERNAL, PROPERTY_STAGE, node.getStageName());
            context.addString(NAMESPACE_INTERNAL, PROPERTY_STATUS, recipeResult.getState().getString());
        }

        context.addString(NAMESPACE_INTERNAL, prefix + PROPERTY_AGENT, node.getHostSafe());
        if(result != null)
        {
            context.addString(NAMESPACE_INTERNAL, prefix + PROPERTY_RECIPE, recipeResult.getRecipeNameSafe());
            context.addString(NAMESPACE_INTERNAL, prefix + PROPERTY_STATUS, recipeResult.getState().getString());
            context.addString(NAMESPACE_INTERNAL, prefix + PROPERTY_DIRECTORY, recipeResult.getAbsoluteOutputDir(configurationManager.getDataDirectory()).getAbsolutePath());

            for(CommandResult command: recipeResult.getCommandResults())
            {
                addCommandProperties(context, node, command, configurationManager, includeName);
            }
        }
    }

    private static void addCommandProperties(ExecutionContext context, RecipeResultNode node, CommandResult commandResult, MasterConfigurationManager configurationManager, boolean includeName)
    {
        String stageName = node.getStageName();
        String commandName = commandResult.getCommandName();
        String prefix = "";

        if(includeName)
        {
            prefix = "stage." + stageName + ".";
        }

        prefix += "command." + commandName + ".";

        context.addString(NAMESPACE_INTERNAL, prefix + PROPERTY_STATUS, commandResult.getState().getString());
        File outputDir = commandResult.getAbsoluteOutputDir(configurationManager.getDataDirectory());
        if (outputDir != null)
        {
            context.addString(NAMESPACE_INTERNAL, prefix + PROPERTY_DIRECTORY, outputDir.getAbsolutePath());
        }
    }
}
