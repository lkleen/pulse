package com.zutubi.pulse.master;

import com.zutubi.events.Event;
import com.zutubi.pulse.master.events.build.BuildCompletedEvent;
import com.zutubi.pulse.master.model.BuildResult;
import com.zutubi.pulse.master.model.Project;
import com.zutubi.pulse.master.model.ProjectManager;
import com.zutubi.pulse.master.scheduling.EventTriggerFilter;
import com.zutubi.pulse.master.scheduling.TaskExecutionContext;
import com.zutubi.pulse.master.scheduling.Trigger;
import com.zutubi.pulse.master.scheduling.tasks.BuildProjectTask;
import com.zutubi.pulse.master.tove.config.project.DependencyConfiguration;
import com.zutubi.pulse.master.tove.config.project.ProjectConfiguration;
import com.zutubi.pulse.master.tove.config.project.triggers.DependentBuildTriggerConfiguration;
import com.zutubi.util.CollectionUtils;
import com.zutubi.util.Predicate;

/**
 * An event filter that only accepts build completed events associated with a
 * project that is listed as a dependency of the project with which the trigger
 * is configured.
 */
public class DependentBuildEventFilter implements EventTriggerFilter
{
    private ProjectManager projectManager;

    public boolean accept(Trigger trigger, Event event, TaskExecutionContext context)
    {
        if (!isBuildCompletedEvent(event))
        {
            return false;
        }

        // the project in which this trigger is configured.
        ProjectConfiguration projectConfig = projectManager.getProjectConfig(trigger.getProject(), false);
        if (projectConfig == null)
        {
            // This project is invalid or does not exist, hence we are not in a position to
            // trigger a build.  No point continuing..
            return false;
        }

        BuildCompletedEvent buildCompletedEvent = (BuildCompletedEvent) event;
        BuildResult result = buildCompletedEvent.getBuildResult();

        if (!result.succeeded())
        {
            return false;
        }

        DependentBuildTriggerConfiguration config = (DependentBuildTriggerConfiguration) trigger.getConfig();
        if (config.isPropagateStatus())
        {
            context.put(BuildProjectTask.PARAM_STATUS, result.getStatus());
        }

        if (config.isPropagateVersion())
        {
            context.put(BuildProjectTask.PARAM_VERSION, result.getVersion());
            context.put(BuildProjectTask.PARAM_VERSION_PROPAGATED, true);
        }

        context.put(BuildProjectTask.PARAM_DEPENDENT, true);
        context.put(BuildProjectTask.PARAM_META_BUILD_ID, buildCompletedEvent.getBuildResult().getMetaBuildId());
        
        final Project builtProject = result.getProject();

        // Return true iff the triggers project contains a dependency to the built project.
        return CollectionUtils.contains(projectConfig.getDependencies().getDependencies(), new Predicate<DependencyConfiguration>()
        {
            public boolean satisfied(DependencyConfiguration dependency)
            {
                return dependency.getProject().getProjectId() == builtProject.getId();
            }
        });
    }

    private boolean isBuildCompletedEvent(Event event)
    {
        return (event instanceof BuildCompletedEvent);
    }

    public void setProjectManager(ProjectManager projectManager)
    {
        this.projectManager = projectManager;
    }
}