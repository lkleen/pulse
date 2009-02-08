package com.zutubi.pulse;

import com.zutubi.pulse.bootstrap.ComponentContext;
import com.zutubi.pulse.bootstrap.MasterConfigurationManager;
import com.zutubi.pulse.core.Bootstrapper;
import com.zutubi.pulse.core.BuildException;
import com.zutubi.pulse.core.BuildRevision;
import com.zutubi.pulse.core.RecipeRequest;
import com.zutubi.pulse.core.model.*;
import com.zutubi.pulse.events.AsynchronousDelegatingListener;
import com.zutubi.pulse.events.Event;
import com.zutubi.pulse.events.EventListener;
import com.zutubi.pulse.events.EventManager;
import com.zutubi.pulse.events.build.*;
import com.zutubi.pulse.model.*;
import com.zutubi.pulse.scheduling.quartz.TimeoutRecipeJob;
import com.zutubi.pulse.scm.FileStatus;
import com.zutubi.pulse.scm.SCMException;
import com.zutubi.pulse.scm.SCMServer;
import com.zutubi.pulse.scm.SCMServerUtils;
import com.zutubi.pulse.services.ServiceTokenManager;
import com.zutubi.pulse.util.Constants;
import com.zutubi.pulse.util.FileSystemUtils;
import com.zutubi.pulse.util.TimeStamps;
import com.zutubi.pulse.util.TreeNode;
import com.zutubi.pulse.util.logging.Logger;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 *
 */
public class BuildController implements EventListener
{
    private static final Logger LOG = Logger.getLogger(BuildController.class);

    static final String ALLOW_PERSONAL_BUILDS = "ALLOW_PERSONAL_BUILDS";
    static final String PULSE_RESTRICT_PERSONAL_BUILDS = "pulse.restrict.personal.builds";

    private static final String TIMEOUT_TRIGGER_GROUP = "timeout";

    private AbstractBuildRequestEvent request;
    private Project project;
    private BuildSpecification specification;
    private EventManager eventManager;
    private ProjectManager projectManager;
    private UserManager userManager;
    private BuildManager buildManager;
    private TestManager testManager;
    private MasterConfigurationManager configurationManager;
    private RecipeQueue queue;
    private RecipeResultCollector collector;
    private BuildTree tree;
    private BuildResult buildResult;
    private AsynchronousDelegatingListener asyncListener;
    private List<TreeNode<RecipeController>> executingControllers = new LinkedList<TreeNode<RecipeController>>();
    private int pendingRecipes = 0;
    private Scheduler quartzScheduler;
    private ServiceTokenManager serviceTokenManager;
    private BuildResult previousSuccessful;
    private List<ResourceProperty> buildProperties;

    public BuildController(AbstractBuildRequestEvent event, BuildSpecification specification, EventManager eventManager, ProjectManager projectManager, UserManager userManager, BuildManager buildManager, TestManager testManager, RecipeQueue queue, RecipeResultCollector collector, Scheduler quartScheduler, MasterConfigurationManager configManager, ServiceTokenManager serviceTokenManager)
    {
        this.request = event;
        this.project = event.getProject();
        this.specification = specification;
        this.eventManager = eventManager;
        this.projectManager = projectManager;
        this.userManager = userManager;
        this.buildManager = buildManager;
        this.testManager = testManager;
        this.queue = queue;
        this.collector = collector;
        this.quartzScheduler = quartScheduler;
        this.asyncListener = new AsynchronousDelegatingListener(this);
        this.configurationManager = configManager;
        this.serviceTokenManager = serviceTokenManager;
    }

    public void run()
    {
        createBuildTree();

        // Fail early if things are not as expected.
        if (!buildResult.isPersistent())
        {
            throw new RuntimeException("Build result must be a persistent instance.");
        }

        MasterBuildPaths paths = new MasterBuildPaths(configurationManager);
        File buildDir = paths.getBuildDir(buildResult);
        buildResult.setAbsoluteOutputDir(configurationManager.getDataDirectory(), buildDir);
        buildResult.queue();
        buildManager.save(buildResult);

        // We handle this event ourselves: this ensures that all processing of
        // the build from this point forth is handled by the single thread in
        // our async listener.  Basically, given events could be coming from
        // anywhere, even for different builds, it is much safer to ensure we
        // *only* use that thread after we have registered the listener.
        eventManager.register(asyncListener);
        eventManager.publish(new BuildCommencedEvent(this, buildResult));
    }

    public BuildTree createBuildTree()
    {
        tree = new BuildTree();

        TreeNode<RecipeController> root = tree.getRoot();
        buildResult = request.createResult(projectManager, userManager);
        buildManager.save(buildResult);
        previousSuccessful = getPreviousSuccessfulBuild();
        buildProperties = specification.copyProperties();
        buildProperties.add(new ResourceProperty("project", project.getName()));
        buildProperties.add(new ResourceProperty("specification", specification.getName()));
        
        configure(root, buildResult.getRoot(), specification.getRoot());

        return tree;
    }

    private BuildResult getPreviousSuccessfulBuild()
    {
        BuildResult previousSuccessful = null;
        List<BuildResult> previousSuccess = buildManager.querySpecificationBuilds(project, specification.getPname(), new ResultState[] { ResultState.SUCCESS }, -1, -1, 0, 1, true, true);
        if (previousSuccess.size() > 0)
        {
            previousSuccessful = previousSuccess.get(0);
        }
        return previousSuccessful;
    }

    private void configure(TreeNode<RecipeController> rcNode, RecipeResultNode resultNode, BuildSpecificationNode specNode)
    {
        for (BuildSpecificationNode node : specNode.getChildren())
        {
            BuildStage stage = node.getStage();
            RecipeResult recipeResult = new RecipeResult(stage.getRecipe());
            RecipeResultNode childResultNode = new RecipeResultNode(stage.getPname(), recipeResult);
            resultNode.addChild(childResultNode);
            buildManager.save(resultNode);

            MasterBuildPaths paths = new MasterBuildPaths(configurationManager);
            File recipeOutputDir = paths.getOutputDir(buildResult, recipeResult.getId());
            recipeResult.setAbsoluteOutputDir(configurationManager.getDataDirectory(), recipeOutputDir);

            boolean incremental = !request.isPersonal() && specification.getCheckoutScheme() == BuildSpecification.CheckoutScheme.INCREMENTAL_UPDATE;
            List<ResourceProperty> recipeProperties = new LinkedList<ResourceProperty>(buildProperties);
            RecipeRequest recipeRequest = new RecipeRequest(project.getName(), specification.getName(), recipeResult.getId(), stage.getRecipe(), incremental, true, specification.getRetainWorkingCopy(), getResourceRequirements(node, Boolean.getBoolean(PULSE_RESTRICT_PERSONAL_BUILDS)), recipeProperties);
            RecipeDispatchRequest dispatchRequest = new RecipeDispatchRequest(specification, stage.getHostRequirements(), request.getRevision(), recipeRequest, buildResult);
            DefaultRecipeLogger logger = new DefaultRecipeLogger(new File(paths.getRecipeDir(buildResult, recipeResult.getId()), RecipeResult.RECIPE_LOG));
            RecipeResultNode previousRecipe = previousSuccessful == null ? null : previousSuccessful.findResultNode(stage.getPname());
            RecipeController rc = new RecipeController(buildResult, node, childResultNode, dispatchRequest, recipeProperties, request.isPersonal(), incremental, previousRecipe, logger, collector, queue, buildManager, serviceTokenManager);
            TreeNode<RecipeController> child = new TreeNode<RecipeController>(rc);
            rcNode.add(child);
            pendingRecipes++;
            configure(child, childResultNode, node);
        }
    }

    List<ResourceRequirement> getResourceRequirements(BuildSpecificationNode node, boolean restrictPersonalBuilds)
    {
        List<ResourceRequirement> requirements = new LinkedList<ResourceRequirement>();
        requirements.addAll(specification.getRoot().getResourceRequirements());
        requirements.addAll(node.getResourceRequirements());
        if (needsPersonalBuildResource(restrictPersonalBuilds))
        {
            requirements.add(new ResourceRequirement(ALLOW_PERSONAL_BUILDS, null));
        }
        return requirements;
    }

    boolean needsPersonalBuildResource(boolean restrictPersonalBuilds)
    {
        return restrictPersonalBuilds && request.isPersonal();
    }

    public void handleEvent(Event evt)
    {
        // Add an event filter here... we are only interested in events that belong to the project
        // that we are dealing with. Q: How do we identify our events..
        try
        {
            if (evt instanceof BuildCommencedEvent)
            {
                BuildCommencedEvent e = (BuildCommencedEvent) evt;
                if (e.getResult() == buildResult)
                {
                    handleBuildCommenced();
                }
            }
            else if (evt instanceof BuildTerminationRequestEvent)
            {
                handleBuildTerminationRequest((BuildTerminationRequestEvent) evt);
            }
            else if (evt instanceof RecipeTimeoutEvent)
            {
                handleRecipeTimeout((RecipeTimeoutEvent) evt);
            }
            else if (evt instanceof RecipeCollectingEvent || evt instanceof RecipeCollectedEvent)
            {
                // Ignore these.
            }
            else if (evt instanceof RecipeEvent)
            {
                RecipeEvent e = (RecipeEvent) evt;
                handleRecipeEvent(e);
            }
            else
            {
                LOG.warning("Build controller received unexpected event of type " + evt.getClass().getName());
            }
        }
        catch (BuildException e)
        {
            buildResult.error(e);
            completeBuild();
        }
        catch (Exception e)
        {
            LOG.severe(e);
            buildResult.error("Unexpected error: " + e.getMessage());
            completeBuild();
        }
    }

    private void handleBuildCommenced()
    {
        // Reload any persistent instances we use, as we are crossing a
        // thread boundary.  The project can't be deleted while a build is in
        // progress, but the specification *might*.
        project = projectManager.getProject(project.getId());
        specification = project.getBuildSpecification(specification.getId());
        if (specification == null)
        {
            throw new BuildException("Build specification deleted during build");
        }

        // It is important that this directory is created *after* the build
        // result is commenced and saved to the database, so that the
        // database knows of the possibility of some other persistent
        // artifacts, even if an error occurs very early in the build.
        File buildDir = buildResult.getAbsoluteOutputDir(configurationManager.getDataDirectory());
        if (!buildDir.mkdirs())
        {
            throw new BuildException("Unable to create build directory '" + buildDir.getAbsolutePath() + "'");
        }

        if (!buildManager.isSpaceAvailableForBuild())
        {
            throw new BuildException("Insufficient database space to run build.  Consider adding more cleanup rules to remove old build information");
        }

        tree.prepare(buildResult);

        // execute the first level of recipe controllers...
        initialiseNodes(new BootstrapperCreator()
        {
            public Bootstrapper create()
            {
                Bootstrapper initialBootstrapper;
                boolean checkoutOnly = request.isPersonal() || specification.getCheckoutScheme() == BuildSpecification.CheckoutScheme.CLEAN_CHECKOUT;
                if (checkoutOnly)
                {
                    initialBootstrapper = new CheckoutBootstrapper(project.getName(), specification.getName(), project.getScm(), request.getRevision(), false);
                    if (request.isPersonal())
                    {
                        initialBootstrapper = createPersonalBuildBootstrapper(initialBootstrapper);
                    }
                }
                else
                {
                    initialBootstrapper = new ProjectRepoBootstrapper(project.getName(), specification.getName(), project.getScm(), request.getRevision());
                }
                return initialBootstrapper;
            }
        }, tree.getRoot().getChildren());
    }

    private Bootstrapper createPersonalBuildBootstrapper(Bootstrapper initialBootstrapper)
    {
        // TODO: preferrable to move this out (maybe to the request)
        PersonalBuildRequestEvent pbr = ((PersonalBuildRequestEvent) request);
        SCMServer scm = null;
        try
        {
            scm = project.getScm().createServer();
            FileStatus.EOLStyle localEOL = scm.getEOLPolicy();
            initialBootstrapper = new PatchBootstrapper(initialBootstrapper, pbr.getUser().getId(), pbr.getNumber(), localEOL);
        }
        catch (SCMException e)
        {
            throw new BuildException("Unable to determine SCM end-of-line policy: " + e.getMessage(), e);
        }
        finally
        {
            SCMServerUtils.close(scm);
        }
        return initialBootstrapper;
    }

    private String getTriggerName(long recipeId)
    {
        return String.format("recipe-%d", recipeId);
    }

    private void handleBuildTerminationRequest(BuildTerminationRequestEvent event)
    {
        long id = event.getBuildId();

        if (id == buildResult.getId() || id == -1)
        {
            // Tell every running recipe to stop, and mark the build terminating
            // (so it will go into the error state on completion).
            buildResult.terminate(event.isTimeout());
            List<TreeNode<RecipeController>> completedNodes = new ArrayList<TreeNode<RecipeController>>(executingControllers.size());

            if (executingControllers.size() > 0)
            {
                for (TreeNode<RecipeController> controllerNode : executingControllers)
                {
                    RecipeController controller = controllerNode.getData();
                    controller.terminateRecipe(event.isTimeout());
                    if (checkControllerStatus(controller, false))
                    {
                        completedNodes.add(controllerNode);
                    }
                }

                buildManager.save(buildResult);
                executingControllers.removeAll(completedNodes);
            }

            if (executingControllers.size() == 0)
            {
                completeBuild();
            }
        }
    }

    private void handleRecipeTimeout(RecipeTimeoutEvent event)
    {
        TreeNode<RecipeController> found = null;
        for (TreeNode<RecipeController> controllerNode : executingControllers)
        {
            RecipeController controller = controllerNode.getData();
            if (controller.getResult().getId() == event.getRecipeId())
            {
                found = controllerNode;
                break;
            }
        }

        if (found != null)
        {
            RecipeController controller = found.getData();
            controller.terminateRecipe(true);
            if (checkControllerStatus(controller, false))
            {
                executingControllers.remove(found);
                if (executingControllers.size() == 0)
                {
                    completeBuild();
                }
            }
        }
    }

    private void initialiseNodes(BootstrapperCreator creator, List<TreeNode<RecipeController>> nodes)
    {
        // Important to add them all first as a failure during initialisation
        // will test if there are other executing controllers (if not the
        // build is finished).
        for (TreeNode<RecipeController> node : nodes)
        {
            executingControllers.add(node);
        }

        for (TreeNode<RecipeController> node : nodes)
        {
            node.getData().initialise(creator.create());
            checkNodeStatus(node);
        }
    }

    private void handleRecipeEvent(RecipeEvent e)
    {
        RecipeController controller;
        TreeNode<RecipeController> foundNode = null;

        for (TreeNode<RecipeController> node : executingControllers)
        {
            controller = node.getData();
            if (controller.handleRecipeEvent(e))
            {
                foundNode = node;
                break;
            }
        }

        if (foundNode != null)
        {
            // If we got here we are sure that the event was for one of our
            // recipes.
            if (e instanceof RecipeCommencedEvent)
            {
                pendingRecipes--;

                if (pendingRecipes == 0)
                {
                    handleLastCommenced();
                }

                if (specification.getTimeout() != BuildSpecification.TIMEOUT_NEVER)
                {
                    scheduleTimeout(e.getRecipeId());
                }
            }
            else if (e instanceof RecipeDispatchedEvent)
            {
                if (!buildResult.commenced())
                {
                    handleFirstDispatch(foundNode.getData());
                }
            }
            else if (e instanceof RecipeCompletedEvent || e instanceof RecipeErrorEvent)
            {
                try
                {
                    // during a system shutdown, the scheduler is shutdown before the
                    // builds are completed. This makes it unnecessary to unschedule the job.
                    if (!quartzScheduler.isShutdown())
                    {
                        quartzScheduler.unscheduleJob(getTriggerName(e.getRecipeId()), TIMEOUT_TRIGGER_GROUP);
                    }
                }
                catch (SchedulerException ex)
                {
                    LOG.warning("Unable to unschedule timeout trigger: " + ex.getMessage(), ex);
                }

                if (e instanceof RecipeCompletedEvent)
                {
                    RecipeCompletedEvent completedEvent = (RecipeCompletedEvent) e;
                    String version = completedEvent.getBuildVersion();
                    if (version != null)
                    {
                        buildResult.setVersion(version);
                        buildManager.save(buildResult);
                    }
                }
            }

            checkNodeStatus(foundNode);
        }
    }

    private void handleLastCommenced()
    {
        // We can now make a more accurate estimate of our remaining running
        // time, as there are no more queued recipes.
        long longestRemaining = 0;

        for (RecipeController controller : tree)
        {
            TimeStamps stamps = controller.getResult().getStamps();
            if (stamps.hasEstimatedEndTime())
            {
                long remaining = stamps.getEstimatedTimeRemaining();
                if (remaining > longestRemaining)
                {
                    longestRemaining = remaining;
                }
            }
        }

        TimeStamps buildStamps = buildResult.getStamps();
        long estimatedEnd = System.currentTimeMillis() + longestRemaining;
        if (estimatedEnd > buildStamps.getStartTime())
        {
            buildStamps.setEstimatedRunningTime(estimatedEnd - buildStamps.getStartTime());
        }
    }

    /**
     * Called when the first recipe for this build is dispatched.  It is at
     * this point that the build is said to have commenced.
     */
    private void handleFirstDispatch(RecipeController controller)
    {
        RecipeDispatchRequest dispatchRequest = controller.getDispatchRequest();
        RecipeRequest request = dispatchRequest.getRequest();

        try
        {
            FileSystemUtils.createFile(new File(buildResult.getAbsoluteOutputDir(configurationManager.getDataDirectory()), BuildResult.PULSE_FILE), request.getPulseFileSource());
        }
        catch (IOException e)
        {
            LOG.warning("Unable to save pulse file for build: " + e.getMessage(), e);
        }

        if (!buildResult.isPersonal())
        {
            getChanges(dispatchRequest.getRevision());
        }

        buildResult.commence(dispatchRequest.getRevision().getTimestamp());
        if (previousSuccessful != null)
        {
            buildResult.getStamps().setEstimatedRunningTime(previousSuccessful.getStamps().getElapsed());
        }
        buildManager.save(buildResult);
    }

    private void getChanges(BuildRevision buildRevision)
    {
        Revision revision = buildRevision.getRevision();
        BuildScmDetails scmDetails = new BuildScmDetails(revision);
        buildResult.setScmDetails(scmDetails);

        if (!buildResult.isUserRevision())
        {
            Scm scm = project.getScm();
            Revision previousRevision = buildManager.getPreviousRevision(project, specification.getPname());

            if (previousRevision != null)
            {
                SCMServer server = null;
                try
                {
                    server = scm.createServer();
                    getChangeSince(server, previousRevision, revision);
                }
                catch (SCMException e)
                {
                    LOG.warning("Unable to retrieve changelist details from SCM server: " + e.getMessage(), e);
                }
                finally
                {
                    SCMServerUtils.close(server);
                }
            }
        }
    }

    private List<Changelist> getChangeSince(SCMServer server, Revision previousRevision, Revision revision) throws SCMException
    {
        List<Changelist> result = new LinkedList<Changelist>();
        List<Changelist> scmChanges = server.getChanges(previousRevision, revision, "");

        for (Changelist change : scmChanges)
        {
            change.setProjectId(buildResult.getProject().getId());
            change.setResultId(buildResult.getId());
            buildManager.save(change);
            result.add(change);
        }

        return result;
    }

    private void scheduleTimeout(long recipeId)
    {
        String name = getTriggerName(recipeId);
        Date time = new Date(System.currentTimeMillis() + specification.getTimeout() * Constants.MINUTE);

        Trigger timeoutTrigger = new SimpleTrigger(name, TIMEOUT_TRIGGER_GROUP, time);
        timeoutTrigger.setJobName(FatController.TIMEOUT_JOB_NAME);
        timeoutTrigger.setJobGroup(FatController.TIMEOUT_JOB_GROUP);
        timeoutTrigger.getJobDataMap().put(TimeoutRecipeJob.PARAM_BUILD_ID, buildResult.getId());
        timeoutTrigger.getJobDataMap().put(TimeoutRecipeJob.PARAM_RECIPE_ID, recipeId);

        try
        {
            quartzScheduler.scheduleJob(timeoutTrigger);
        }
        catch (SchedulerException e)
        {
            LOG.severe("Unable to schedule build timeout trigger: " + e.getMessage(), e);
        }
    }

    private boolean checkControllerStatus(RecipeController controller, boolean collect)
    {
        if (controller.isFinished())
        {
            eventManager.publish(new RecipeCollectingEvent(this, controller.getResult().getId()));

            if (collect)
            {
                controller.collect(buildResult, specification.getRetainWorkingCopy());
            }

            controller.cleanup(buildResult);
            controller.runPostStageActions();

            eventManager.publish(new RecipeCollectedEvent(this, controller.getResult().getId()));
            return true;
        }

        return false;
    }

    private void checkNodeStatus(TreeNode<RecipeController> node)
    {
        final RecipeController controller = node.getData();
        if (checkControllerStatus(controller, true))
        {
            executingControllers.remove(node);

            RecipeResult result = controller.getResult();
            if (result.succeeded())
            {
                initialiseNodes(new BootstrapperCreator()
                {
                    public Bootstrapper create()
                    {
                        return controller.getChildBootstrapper();
                    }
                }, node.getChildren());
            }
            else if (result.failed())
            {
                buildResult.addFeature(Feature.Level.ERROR, "Recipe " + result.getRecipeNameSafe() + " failed");
            }
            else if (result.errored())
            {
                buildResult.addFeature(Feature.Level.ERROR, "Error executing recipe " + result.getRecipeNameSafe());
            }

            buildManager.save(buildResult);
        }

        if (executingControllers.size() == 0)
        {
            completeBuild();
        }
    }

    private void completeBuild()
    {
        abortUnfinishedRecipes();

        // FIXME: If there is an SQL problem while saving the build result, the build becomes stuck and the server
        // needs to be restarted to clear it up.  To prevent the need for server restarts, we catch and log the exception
        // and continue.  This leaves the build result in an incorrect state, but will allow builds to continue. The
        // builds will be cleaned up next time the server restarts.  THIS IS ONLY A TEMPORARY FIX UNTIL WE WORK OUT
        // WHAT IS CAUSING THE SQL PROBLEMS (DEADLOCKS, STALE SESSIONS) IN THE FIRST PLACE.
        // Unfortunately, if we can not write to the db, then we are a little stuffed.
        try
        {
            buildResult.setHasWorkDir(specification.getRetainWorkingCopy());
            buildResult.complete();

            if (!request.isPersonal())
            {
                for (PostBuildAction action : buildResult.getProject().getPostBuildActions())
                {
                    ComponentContext.autowire(action);
                    action.execute(buildResult, null, buildProperties);
                }
            }

            // calculate the feature counts at the end of the build so that the result hierarchy does not need to
            // be traversed when this information is required.
            buildResult.calculateFeatureCounts();

            long start = System.currentTimeMillis();
            testManager.index(buildResult);
            long duration = System.currentTimeMillis() - start;
            if (duration > 300000)
            {
                LOG.warning("Test case indexing for project %s specification %s took %f seconds", project.getName(), specification.getName(), duration / 1000.0);
            }

            buildManager.save(buildResult);
        }
        catch (Exception e)
        {
            LOG.severe("Failed to persist the completed build result. Reason: " + e.getMessage(), e);
        }

        eventManager.unregister(asyncListener);
        eventManager.publish(new BuildCompletedEvent(this, buildResult));
        asyncListener.stop(true);
    }

    private void abortUnfinishedRecipes()
    {
        buildResult.abortUnfinishedRecipes();
        for (TreeNode<RecipeController> controllerNode: executingControllers)
        {
            eventManager.publish(new RecipeAbortedEvent(this, controllerNode.getData().getResult().getId()));
        }
    }

    public Class[] getHandledEvents()
    {
        return new Class[] { BuildCommencedEvent.class, RecipeEvent.class, BuildTerminationRequestEvent.class, RecipeTimeoutEvent.class };
    }

    public long getBuildId()
    {
        return buildResult.getId();
    }

    public void setConfigurationManager(MasterConfigurationManager configurationManager)
    {
        this.configurationManager = configurationManager;
    }

    private static interface BootstrapperCreator
    {
        Bootstrapper create();
    }
}