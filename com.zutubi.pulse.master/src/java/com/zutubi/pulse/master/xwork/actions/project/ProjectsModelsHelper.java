package com.zutubi.pulse.master.xwork.actions.project;

import com.zutubi.i18n.Messages;
import com.zutubi.pulse.master.model.*;
import com.zutubi.pulse.master.tove.config.MasterConfigurationRegistry;
import com.zutubi.pulse.master.tove.config.project.ProjectConfiguration;
import com.zutubi.pulse.master.tove.config.project.ProjectConfigurationActions;
import com.zutubi.pulse.master.tove.config.user.ProjectsSummaryConfiguration;
import com.zutubi.pulse.master.webwork.Urls;
import com.zutubi.tove.config.ConfigurationTemplateManager;
import com.zutubi.tove.config.TemplateHierarchy;
import com.zutubi.tove.config.TemplateNode;
import com.zutubi.tove.security.AccessManager;
import com.zutubi.tove.type.record.RecordManager;
import com.zutubi.util.CollectionUtils;
import com.zutubi.util.Predicate;
import com.zutubi.util.TruePredicate;

import java.util.*;

/**
 * A shared helper class that can take a set of projects and groups and output
 * corresponding {@link ProjectsModel} instances.
 */
public class ProjectsModelsHelper
{
    // Chose as it can't be in a name.  We don't use PathUtils as it ignores
    // empty pieces when joining paths (these are significant here).
    private static final String SEPARATOR = "/";

    private ProjectManager projectManager;
    private BuildManager buildManager;
    private ConfigurationTemplateManager configurationTemplateManager;
    private RecordManager recordManager;
    private AccessManager accessManager;

    private ProjectsModelSorter sorter = new ProjectsModelSorter();

    public List<ProjectsModel> createProjectsModels(User loggedInUser, ProjectsSummaryConfiguration configuration, Urls urls, boolean showUngrouped)
    {
        return createProjectsModels(loggedInUser, configuration, Collections.<LabelProjectTuple>emptySet(), urls, new TruePredicate<Project>(), new TruePredicate<ProjectGroup>(), showUngrouped);
    }

    /**
     * Creates a list of models, one for each group of projects to be shown.
     *
     * @param loggedInUser     the user that is currently logged in, if any
     * @param configuration    configuration controlling overall layout (e.g.
     *                         are hierarchies shown)
     * @param tuples           set of (label,project) tuples, one for each
     *                         collapsed group/template
     * @param urls             used to generate URLs in returned models
     * @param projectPredicate only projects satisfying this predicate are
     *                         included
     * @param groupPredicate   only groups satisfying this predicate are
     *                         included
     * @param showUngrouped    if true, shown projects not included in a shown
     *                         group will be shown in a special "ungrouped
     *                         projects" group
     * @return a list of models ready for JSON encoding and sending to a
     *         projects view in the UI 
     */
    public List<ProjectsModel> createProjectsModels(User loggedInUser, ProjectsSummaryConfiguration configuration, Set<LabelProjectTuple> tuples, Urls urls, Predicate<Project> projectPredicate, Predicate<ProjectGroup> groupPredicate, boolean showUngrouped)
    {
        Set<String> collapsed = translateCollapsed(tuples);
        List<Project> projects = CollectionUtils.filter(projectManager.getProjects(false), projectPredicate);
        List<ProjectGroup> groups = CollectionUtils.filter(projectManager.getAllProjectGroups(), groupPredicate);
        TemplateHierarchy hierarchy = configurationTemplateManager.getTemplateHierarchy(MasterConfigurationRegistry.PROJECTS_SCOPE);

        List<ProjectsModel> result = new LinkedList<ProjectsModel>();
        Map<Project, List<BuildResult>> buildCache = new HashMap<Project, List<BuildResult>>();

        for (ProjectGroup group : groups)
        {
            List<Project> groupProjects = CollectionUtils.filter(group.getProjects(), projectPredicate);
            if (!groupProjects.isEmpty())
            {
                ProjectsModel projectsModel = createModel(group.getName(), true, groupProjects, hierarchy, loggedInUser, configuration, collapsed, buildCache, urls);
                result.add(projectsModel);
                projects.removeAll(groupProjects);
            }
        }

        if (showUngrouped && projects.size() > 0)
        {
            // CIB-1550: Only label as ungrouped if there are some other groups.
            Messages messages = Messages.getInstance(ProjectsModelsHelper.class);
            String name = result.size() > 0 ? messages.format("projects.ungrouped") : messages.format("projects.all");
            ProjectsModel projectsModel = createModel(name, false, projects, hierarchy, loggedInUser, configuration, collapsed, buildCache, urls);
            result.add(projectsModel);
        }

        sorter.sort(result);

        return result;
    }

    private Set<String> translateCollapsed(Set<LabelProjectTuple> tuples)
    {
        Set<String> result = new HashSet<String>();
        for (LabelProjectTuple tuple: tuples)
        {
            String project = null;
            if (tuple.isSpecificProject())
            {
                String path = recordManager.getPathForHandle(tuple.getProjectHandle());
                if (path != null)
                {
                    ProjectConfiguration configuration = configurationTemplateManager.getInstance(path, ProjectConfiguration.class);
                    if (configuration != null)
                    {
                        project = configuration.getName();
                    }
                }
            }
            else
            {
                project = "";
            }

            if (project != null)
            {
                result.add(tuple.getLabel() + SEPARATOR + project);
            }
        }

        return result;
    }

    private ProjectsModel createModel(String name, boolean labelled, Collection<Project> projects, TemplateHierarchy hierarchy, User loggedInUser, ProjectsSummaryConfiguration configuration, Set<String> collapsed, Map<Project, List<BuildResult>> buildCache, Urls urls)
    {
        ProjectsModel model = new ProjectsModel(name, labelled, collapsed.contains((labelled ? name : "") + SEPARATOR));
        if (configuration.isHierarchyShown())
        {
            // The group can display all concrete projects plus all of their
            // ancestors (which may overlap).  The ancestors may not define the
            // label, but are included to prevent "holes" in the hierarchy.
            Set<String> includedInGroup = new HashSet<String>();
            for (Project p : projects)
            {
                TemplateNode node = hierarchy.getNodeById(p.getName());
                while (node != null)
                {
                    includedInGroup.add(node.getId());
                    node = node.getParent();
                }
            }

            processLevel(model, model.getRoot(), Arrays.asList(hierarchy.getRoot()), 0, includedInGroup, loggedInUser, configuration, collapsed, buildCache, urls);
        }
        else
        {
            for (Project p : projects)
            {
                boolean canTrigger = accessManager.hasPermission(ProjectConfigurationActions.ACTION_TRIGGER, p);
                boolean prompt = p.getConfig().getOptions().getPrompt();
                boolean canViewSource = accessManager.hasPermission(ProjectConfigurationActions.ACTION_VIEW_SOURCE, p);
                model.getRoot().addChild(new ConcreteProjectModel(model, p, getBuilds(p, configuration, buildCache), loggedInUser, configuration, urls, canTrigger, prompt, canViewSource));
            }
        }

        return model;
    }

    private void processLevel(ProjectsModel group, TemplateProjectModel parentModel, List<TemplateNode> nodes, int depth, Set<String> includedInGroup, User loggedInUser, ProjectsSummaryConfiguration configuration, Set<String> collapsed, Map<Project, List<BuildResult>> buildCache, Urls urls)
    {
        for (TemplateNode node : nodes)
        {
            if (node.isConcrete() || depth >= configuration.getHiddenHierarchyLevels())
            {
                if (includedInGroup.contains(node.getId()))
                {
                    ProjectModel model;
                    String name = node.getId();
                    if (node.isConcrete())
                    {
                        Project project = projectManager.getProject(name, true);
                        List<BuildResult> builds = getBuilds(project, configuration, buildCache);
                        boolean canTrigger = accessManager.hasPermission(ProjectConfigurationActions.ACTION_TRIGGER, project);
                        boolean prompt = project.getConfig().getOptions().getPrompt();
                        boolean canViewSource = accessManager.hasPermission(ProjectConfigurationActions.ACTION_VIEW_SOURCE, project);
                        model = new ConcreteProjectModel(group, project, builds, loggedInUser, configuration, urls, canTrigger, prompt, canViewSource);
                    }
                    else
                    {
                        String groupName = group.isLabelled() ? group.getGroupName() : "";
                        TemplateProjectModel template = new TemplateProjectModel(group, name, collapsed.contains(groupName + SEPARATOR + name));
                        processLevel(group, template, node.getChildren(), depth + 1, includedInGroup, loggedInUser, configuration, collapsed, buildCache, urls);
                        model = template;
                    }

                    parentModel.addChild(model);
                }
            }
            else
            {
                processLevel(group, parentModel, node.getChildren(), depth + 1, includedInGroup, loggedInUser, configuration, collapsed, buildCache, urls);
            }
        }
    }

    /**
     * Get a list of builds for the specified project.  The number of results returned is at least 2 in length
     * since we require at least 2 build results (one is possibly active) to be able to determine the health of
     * the project.  This assumes that only one active build per project at a time.
     *
     * @param project       the project in question
     * @param configuration the project summary configuration
     * @param cache         cache of builds looked up so far in this model creation (for consistency and
     *                      efficiency)
     * @return a list of build results.
     */
    private List<BuildResult> getBuilds(Project project, ProjectsSummaryConfiguration configuration, Map<Project, List<BuildResult>> cache)
    {
        List<BuildResult> result = cache.get(project);
        if (result == null)
        {
            result = buildManager.getLatestBuildResultsForProject(project, Math.max(2, configuration.getBuildsPerProject()));
            cache.put(project, result);
        }

        return result;
    }

    public void setProjectManager(ProjectManager projectManager)
    {
        this.projectManager = projectManager;
    }

    public void setBuildManager(BuildManager buildManager)
    {
        this.buildManager = buildManager;
    }

    public void setConfigurationTemplateManager(ConfigurationTemplateManager configurationTemplateManager)
    {
        this.configurationTemplateManager = configurationTemplateManager;
    }

    public void setSorter(ProjectsModelSorter sorter)
    {
        this.sorter = sorter;
    }

    public void setRecordManager(RecordManager recordManager)
    {
        this.recordManager = recordManager;
    }

    public void setAccessManager(AccessManager accessManager)
    {
        this.accessManager = accessManager;
    }
}
