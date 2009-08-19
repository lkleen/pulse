package com.zutubi.pulse.acceptance.dependencies;

import com.zutubi.util.CollectionUtils;
import com.zutubi.util.Predicate;
import com.zutubi.util.StringUtils;
import com.zutubi.util.Pair;
import static com.zutubi.util.CollectionUtils.asPair;
import com.zutubi.tove.type.record.PathUtils;
import com.zutubi.pulse.master.tove.config.MasterConfigurationRegistry;
import com.zutubi.pulse.master.tove.config.project.triggers.DependentBuildTriggerConfiguration;
import com.zutubi.pulse.master.tove.config.project.BuildStageConfiguration;
import com.zutubi.pulse.master.tove.config.project.DependencyConfigurationRevisionOptionProvider;
import com.zutubi.pulse.master.tove.config.project.DependencyConfiguration;
import com.zutubi.pulse.acceptance.Constants;
import com.zutubi.pulse.acceptance.XmlRpcHelper;
import com.zutubi.pulse.core.engine.RecipeConfiguration;
import com.zutubi.pulse.core.engine.api.ResultState;
import com.zutubi.pulse.core.commands.api.FileArtifactConfiguration;

import java.util.*;
import java.lang.reflect.Array;

/**
 * The project model used by these tests to simplify management of the test configuration.
 */
public abstract class Project
{
    private String name;
    private String org;
    private String status;
    private String version;
    private List<Dependency> dependencies = new LinkedList<Dependency>();

    private List<Stage> stages = new LinkedList<Stage>();
    private List<Recipe> recipes = new LinkedList<Recipe>();

    private boolean propagateStatus = false;
    private boolean propagateVersion = false;

    private String retrievalPattern = "lib/[artifact].[ext]";
    protected XmlRpcHelper xmlRpcHelper;

    public Project(XmlRpcHelper xmlRpcHelper, String name)
    {
        this.setName(name);
        addStage("default");
        addRecipe("default");
        this.xmlRpcHelper = xmlRpcHelper;
    }

    public Project(XmlRpcHelper xmlRpcHelper, String name, String org)
    {
        this(xmlRpcHelper, name);
        this.setOrg(org);
        this.xmlRpcHelper = xmlRpcHelper;
    }

    public Recipe addRecipe(String recipeName)
    {
        Recipe recipe = new Recipe(this, recipeName);
        this.recipes.add(recipe);
        return recipe;
    }

    public Stage addStage(String stageName)
    {
        Stage stage = new Stage(this, stageName);
        this.stages.add(stage);
        return stage;
    }

    public Dependency addDependency(Project dependency)
    {
        Dependency instance = new Dependency(dependency);
        dependencies.add(instance);
        return instance;
    }

    public void addDependency(Dependency dependnecy)
    {
        dependencies.add(dependnecy);
    }

    public Stage getDefaultStage()
    {
        return getStage("default");
    }

    public Artifact addArtifact(String artifact)
    {
        return getRecipe("default").addArtifact(artifact);
    }

    public List<Artifact> addArtifacts(String... artifacts)
    {
        return getRecipe("default").addArtifacts(artifacts);
    }

    public void setRetrievalPattern(String retrievalPattern)
    {
        this.retrievalPattern = retrievalPattern;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getOrg()
    {
        return org;
    }

    public void setOrg(String org)
    {
        this.org = org;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }

    public boolean isPropagateStatus()
    {
        return propagateStatus;
    }

    public void setPropagateStatus(boolean propagateStatus)
    {
        this.propagateStatus = propagateStatus;
    }

    public boolean isPropagateVersion()
    {
        return propagateVersion;
    }

    public void setPropagateVersion(boolean b)
    {
        this.propagateVersion = b;
    }

    public List<Stage> getStages()
    {
        return stages;
    }

    public Stage getStage(final String stageName)
    {
        return CollectionUtils.find(stages, new Predicate<Stage>()
        {
            public boolean satisfied(Stage stage)
            {
                return stage.getName().equals(stageName);
            }
        });
    }

    public Recipe getRecipe(final String recipeName)
    {
        return CollectionUtils.find(recipes, new Predicate<Recipe>()
        {
            public boolean satisfied(Recipe recipe)
            {
                return recipe.getName().equals(recipeName);
            }
        });
    }

    public List<Recipe> getRecipes()
    {
        return recipes;
    }

    public Recipe getDefaultRecipe()
    {
        return getRecipe("default");
    }

    public List<Dependency> getDependencies()
    {
        return dependencies;
    }

    public String getVersion()
    {
        return version;
    }

    public String getRetrievalPattern()
    {
        return retrievalPattern;
    }

    public int triggerSuccessfulBuild(Pair<String, Object>... options) throws Exception
    {
        int buildNumber = triggerCompleteBuild(options);

        ResultState buildStatus = getBuildStatus(buildNumber);
        if (!ResultState.SUCCESS.equals(buildStatus))
        {
            throw new RuntimeException("Expected success, had " + buildStatus + " instead.");
        }
        return buildNumber;

    }

    public int triggerCompleteBuild(Pair<String, Object>... options) throws Exception
    {
        int number = triggerBuild(options);
        xmlRpcHelper.waitForBuildToComplete(getName(), number);
        return number;
    }

    public int triggerBuild(Pair<String, Object>... options) throws Exception
    {
        Hashtable<String, Object> triggerOptions = new Hashtable<String, Object>();
        if (options != null)
        {
            for (Pair<String, Object> option : options)
            {
                triggerOptions.put(option.getFirst(), option.getSecond());
            }
        }

        int number = xmlRpcHelper.getNextBuildNumber(getName());
        xmlRpcHelper.call("triggerBuild", getName(), triggerOptions);
        return number;
    }

    protected int triggerRebuild(Pair<String, Object>... options) throws Exception
    {
        //noinspection unchecked
        Pair<String, Object>[] args = (Pair<String, Object>[]) Array.newInstance(Pair.class, options.length + 1);
        System.arraycopy(options, 0, args, 0, options.length);
        args[args.length - 1] = asPair("rebuild", (Object)"true");
        return triggerBuild(args);
    }

    public ResultState getBuildStatus(int buildNumber) throws Exception
    {
        Hashtable<String, Object> build = xmlRpcHelper.getBuild(getName(), buildNumber);
        if (build != null)
        {
            return ResultState.fromPrettyString((String) build.get("status"));
        }
        return null;
    }

    public com.zutubi.pulse.master.model.Project.State getState() throws Exception
    {
        return xmlRpcHelper.getProjectState(getName());
    }

    protected void createProject() throws Exception
    {
        Hashtable<String, Object> commandConfig = insertProject();

        insertProjectOrganisation();
        insertDependencies();

        for (Recipe recipe : getRecipes())
        {
            insertRecipe(recipe, commandConfig);
            for (Artifact artifact : recipe.getArtifacts())
            {
                insertArtifact(recipe.getName(), "build", artifact.getName(), artifact.getExtension(), artifact.getArtifactPattern());
            }
        }

        for (Stage stage : getStages())
        {
            // create stage.
            insertStage(stage);

            // setup the rest of the properties configured for the stage.
            for (Map.Entry<String, String> entry : stage.getProperties().entrySet())
            {
                xmlRpcHelper.insertOrUpdateStageProperty(getName(), stage.getName(), entry.getKey(), entry.getValue());
            }
        }

        for (Dependency dependency : getDependencies())
        {
            insertDependency(dependency);
        }

        insertDependentBuildTrigger();
    }

    protected abstract Hashtable<String, Object> insertProject() throws Exception;

    protected void insertDependentBuildTrigger() throws Exception
    {
        String triggersPath = PathUtils.getPath(MasterConfigurationRegistry.PROJECTS_SCOPE, getName(), "triggers");
        Hashtable<String, Object> trigger = xmlRpcHelper.createEmptyConfig(DependentBuildTriggerConfiguration.class);
        trigger.put("name", "dependency trigger");
        trigger.put("propagateStatus", isPropagateStatus());
        trigger.put("propagateVersion", isPropagateVersion());
        xmlRpcHelper.insertConfig(triggersPath, trigger);
    }

    protected void insertProjectOrganisation() throws Exception
    {
        if (StringUtils.stringSet(getOrg()))
        {
            String path = "projects/" + getName();
            Hashtable<String, Object> projectConfig = xmlRpcHelper.getConfig(path);
            projectConfig.put(Constants.Project.ORGANISATION, getOrg());
            xmlRpcHelper.saveConfig(path, projectConfig, true);
        }
    }

    protected void insertRecipe(Recipe recipe, Hashtable<String, Object> commandConfig) throws Exception
    {
        String recipePath = "projects/" + getName() + "/type/recipes/" + recipe.getName();
        if (!xmlRpcHelper.configPathExists(recipePath))
        {
            Hashtable<String, Object> recipeConfig = xmlRpcHelper.createDefaultConfig(RecipeConfiguration.class);
            recipeConfig.put(Constants.Project.MultiRecipeType.NAME, recipe.getName());

            Hashtable<String, Object> commands = new Hashtable<String, Object>();
            commands.put((String)commandConfig.get("name"), commandConfig);
            recipeConfig.put("commands", commands);

            xmlRpcHelper.insertConfig("projects/" + getName() + "/type/recipes", recipeConfig);
        }
    }

    protected void insertStage(Stage stage) throws Exception
    {
        // configure the default stage.
        String stagePath = "projects/" + getName() + "/stages/" + stage.getName();
        if (!xmlRpcHelper.configPathExists(stagePath))
        {
            Hashtable<String, Object> stageConfig = xmlRpcHelper.createDefaultConfig(BuildStageConfiguration.class);
            stageConfig.put(Constants.Project.Stage.NAME, stage.getName());
            stageConfig.put(Constants.Project.Stage.RECIPE, stage.getRecipe().getName());
            xmlRpcHelper.insertConfig("projects/" + getName() + "/stages", stageConfig);
        }
    }

    protected void insertDependencies() throws Exception
    {
        // configure the default stage.
        String dependenciesPath = "projects/" + getName() + "/dependencies";
        Hashtable<String, Object> dependencies = xmlRpcHelper.getConfig(dependenciesPath);
        dependencies.put(Constants.Project.Dependencies.RETRIEVAL_PATTERN, getRetrievalPattern());
        if (StringUtils.stringSet(getStatus()))
        {
            dependencies.put(Constants.Project.Dependencies.STATUS, getStatus());
        }
        if (StringUtils.stringSet(getVersion()))
        {
            dependencies.put(Constants.Project.Dependencies.VERSION, getVersion());
        }
        xmlRpcHelper.saveConfig(dependenciesPath, dependencies, false);
    }

    protected void insertArtifact(String recipe, String command, String artifactName, String artifactExtension, String pattern) throws Exception
    {
        String artifactsPath = "projects/" + getName() + "/type/recipes/" + recipe + "/commands/" + command + "/artifacts";

        Hashtable<String, Object> artifactData = xmlRpcHelper.createDefaultConfig(FileArtifactConfiguration.class);
        artifactData.put("name", artifactName);
        artifactData.put("file", "build/" + artifactName + "." + artifactExtension);
        artifactData.put("publish", true);
        if (pattern != null)
        {
            artifactData.put("artifactPattern", pattern);
        }

        xmlRpcHelper.insertConfig(artifactsPath, artifactData);
    }

    protected void insertDependency(Dependency projectDependency) throws Exception
    {
        // configure the default stage.
        String projectDependenciesPath = "projects/" + getName() + "/dependencies";

        Hashtable<String, Object> projectDependencies = xmlRpcHelper.getConfig(projectDependenciesPath);
        if (!projectDependencies.containsKey("dependencies"))
        {
            projectDependencies.put("dependencies", new Vector<Hashtable<String, Object>>());
        }

        @SuppressWarnings("unchecked")
        Vector<Hashtable<String, Object>> dependencies = (Vector<Hashtable<String, Object>>) projectDependencies.get("dependencies");

        List<String> revisionOptions = new DependencyConfigurationRevisionOptionProvider().getOptions(null, null, null);

        Hashtable<String, Object> dependency = xmlRpcHelper.createEmptyConfig(DependencyConfiguration.class);
        dependency.put("project", "projects/" + projectDependency.getProject().getName());
        if (revisionOptions.contains(projectDependency.getRevision()))
        {
            dependency.put("revision", projectDependency.getRevision());
        }
        else
        {
            dependency.put("revision", DependencyConfiguration.REVISION_CUSTOM);
            dependency.put("customRevision", projectDependency.getRevision());
        }

        dependency.put("allStages", (projectDependency.getStage() == null));
        dependency.put("stages", asStagePaths(projectDependency));
        dependency.put("transitive", projectDependency.isTransitive());
        dependencies.add(dependency);

        xmlRpcHelper.saveConfig(projectDependenciesPath, projectDependencies, true);
    }

    private Vector<String> asStagePaths(Dependency dependency)
    {
        Vector<String> v = new Vector<String>();
        if (dependency.getStage() != null)
        {
            v.add("projects/" + dependency.getProject().getName() + "/stages/" + dependency.getStage());
        }
        return v;
    }
}
