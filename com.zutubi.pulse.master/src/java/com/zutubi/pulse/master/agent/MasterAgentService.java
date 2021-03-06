/* Copyright 2017 Zutubi Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zutubi.pulse.master.agent;

import com.zutubi.pulse.core.PulseExecutionContext;
import com.zutubi.pulse.core.RecipeRequest;
import com.zutubi.pulse.core.engine.api.BuildException;
import com.zutubi.pulse.core.util.process.ProcessWrapper;
import com.zutubi.pulse.master.MasterRecipeRunner;
import com.zutubi.pulse.master.bootstrap.MasterConfigurationManager;
import com.zutubi.pulse.master.model.ResourceManager;
import com.zutubi.pulse.master.tove.config.agent.AgentConfiguration;
import com.zutubi.pulse.servercore.AgentRecipeDetails;
import com.zutubi.pulse.servercore.ServerRecipePaths;
import com.zutubi.pulse.servercore.ServerRecipeService;
import com.zutubi.pulse.servercore.agent.SynchronisationMessage;
import com.zutubi.pulse.servercore.agent.SynchronisationMessageResult;
import com.zutubi.pulse.servercore.agent.SynchronisationTaskRunnerService;
import com.zutubi.pulse.servercore.filesystem.FileInfo;
import com.zutubi.pulse.servercore.filesystem.ToFileInfoFunction;
import com.zutubi.util.bean.ObjectFactory;
import com.zutubi.util.io.FileSystemUtils;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;

/**
 * A service to communicate with agents running within the master.
 */
public class MasterAgentService implements AgentService
{
    private AgentConfiguration agentConfig;

    private ServerRecipeService serverRecipeService;
    private MasterConfigurationManager configurationManager;
    private ObjectFactory objectFactory;
    private ResourceManager resourceManager;
    private SynchronisationTaskRunnerService synchronisationTaskRunnerService;

    public MasterAgentService(AgentConfiguration agentConfig)
    {
        this.agentConfig = agentConfig;
    }

    public AgentConfiguration getAgentConfig()
    {
        return agentConfig;
    }

    public boolean build(RecipeRequest request)
    {
        MasterRecipeRunner recipeRunner = objectFactory.buildBean(MasterRecipeRunner.class, resourceManager.getAgentRepository(agentConfig));
        serverRecipeService.processRecipe(agentConfig.getHandle(), request, recipeRunner);
        return true;
    }

    public void collectResults(AgentRecipeDetails recipeDetails, File outputDest)
    {
        ServerRecipePaths recipePaths = new ServerRecipePaths(recipeDetails, configurationManager.getUserPaths().getData());
        File outputDir = recipePaths.getOutputDir();

        try
        {
            FileSystemUtils.rename(outputDir, outputDest, true);
        }
        catch (IOException e)
        {
            try
            {
                // As a fallback, try copying.  This may be required when
                // different filesystems are involved.
                FileSystemUtils.copy(outputDest, outputDir);
            }
            catch (IOException unused)
            {
                // Report initial error.
                throw new BuildException("Capturing output directory: " + e.getMessage(), e);
            }
        }
    }

    public void cleanup(AgentRecipeDetails recipeDetails)
    {
        // We rename the output dir, so no need to remove it.
        ServerRecipePaths recipePaths = new ServerRecipePaths(recipeDetails, configurationManager.getUserPaths().getData());
        File recipeRoot = recipePaths.getRecipeRoot();

        try
        {
            FileSystemUtils.rmdir(recipeRoot);
        }
        catch (IOException e)
        {
            throw new BuildException("Unable to remove recipe root directory: " + e.getMessage(), e);
        }

        try
        {
            FileSystemUtils.rmdir(recipePaths.getTempDir());
        }
        catch (IOException e)
        {
            throw new BuildException("Unable to remove recipe temp directory: " + e.getMessage(), e);
        }
    }

    public void terminateRecipe(long recipeId)
    {
        serverRecipeService.terminateRecipe(agentConfig.getHandle(), recipeId);
    }

    public List<SynchronisationMessageResult> synchronise(List<SynchronisationMessage> messages)
    {
        return synchronisationTaskRunnerService.synchronise(agentConfig.getAgentStateId(), messages);
    }

    public List<FileInfo> getFileListing(AgentRecipeDetails recipeDetails, String relativePath)
    {
        ServerRecipePaths recipePaths = new ServerRecipePaths(recipeDetails, configurationManager.getUserPaths().getData());
        File path = new File(recipePaths.getBaseDir(), relativePath);
        File[] listing = path.listFiles();
        if (listing != null)
        {
            return newArrayList(transform(asList(listing), new ToFileInfoFunction()));
        }

        return new LinkedList<FileInfo>();
    }

    public FileInfo getFile(AgentRecipeDetails recipeDetails, String relativePath)
    {
        ServerRecipePaths recipePaths = new ServerRecipePaths(recipeDetails, configurationManager.getUserPaths().getData());
        File base = recipePaths.getBaseDir();
        return new FileInfo(new File(base, relativePath));
    }

    public void executeCommand(PulseExecutionContext context, List<String> commandLine, String workingDir, int timeout)
    {
        try
        {
            ProcessWrapper.runCommand(commandLine, workingDir, context, timeout, TimeUnit.SECONDS);
        }
        catch (Exception e)
        {
            throw new BuildException("Could not run command on agent: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof MasterAgentService)
        {
            MasterAgentService other = (MasterAgentService) obj;
            return other.getAgentConfig().equals(agentConfig);
        }

        return false;
    }

    public void setServerRecipeService(ServerRecipeService serverRecipeService)
    {
        this.serverRecipeService = serverRecipeService;
    }

    public void setConfigurationManager(MasterConfigurationManager configurationManager)
    {
        this.configurationManager = configurationManager;
    }

    public void setObjectFactory(ObjectFactory objectFactory)
    {
        this.objectFactory = objectFactory;
    }

    public void setResourceManager(ResourceManager resourceManager)
    {
        this.resourceManager = resourceManager;
    }

    public void setSynchronisationTaskRunnerService(SynchronisationTaskRunnerService synchronisationTaskRunnerService)
    {
        this.synchronisationTaskRunnerService = synchronisationTaskRunnerService;
    }
}
