package com.zutubi.pulse.web.project;

import com.zutubi.pulse.model.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 *
 */
public class ViewProjectsAction extends ProjectActionSupport
{
    private List<ProjectGroup> groups;
    private List<Project> projects;
    private BuildColumns buildColumns;
    private Map<Long, String> projectHealths = new HashMap<Long, String>();
    private Map<Long, BuildResult> latestBuilds = new HashMap<Long, BuildResult>();

    public List<Project> getProjects()
    {
        return projects;
    }

    public List<ProjectGroup> getGroups()
    {
        return groups;
    }

    public String getHealth(Project project)
    {
        return projectHealths.get(project.getId());
    }

    public BuildResult getLatestBuild(Project project)
    {
        return latestBuilds.get(project.getId());
    }

    public BuildColumns getColumns()
    {
        return buildColumns;
    }

    public String execute()
    {
        groups = projectManager.getAllProjectGroupsCached();
        Collections.sort(groups, new NamedEntityComparator());

        projects = getProjectManager().getAllProjectsCached();
        for (Project p : projects)
        {
            String health = "unknown";
            BuildResult latest = null;

            List<BuildResult> buildResults = getBuildManager().getLatestBuildResultsForProject(p, 2);
            switch (buildResults.size())
            {
                case 0:
                {
                    // No latest and health not known
                    break;
                }
                case 1:
                {
                    // A latest, health known if complete
                    latest = buildResults.get(0);
                    if (latest.completed())
                    {
                        health = getHealth(latest);
                    }

                    break;
                }
                case 2:
                {
                    latest = buildResults.get(0);
                    if (latest.completed())
                    {
                        health = getHealth(latest);
                    }
                    else
                    {
                        health = getHealth(buildResults.get(1));
                    }

                    break;
                }
            }

            if (latest != null)
            {
                latestBuilds.put(p.getId(), latest);
            }

            projectHealths.put(p.getId(), health);
        }

        for (ProjectGroup g : groups)
        {
            projects.removeAll(g.getProjects());
        }

        Collections.sort(projects, new NamedEntityComparator());

        User user = getLoggedInUser();
        buildColumns = new BuildColumns(user == null ? User.getDefaultAllProjectsColumns() : user.getAllProjectsColumns(), projectManager);

        return SUCCESS;
    }

    private String getHealth(BuildResult latest)
    {
        String health;
        if (latest.succeeded())
        {
            if(latest.getWarningFeatureCount() > 0)
            {
                health = "warnings";
            }
            else
            {
                health = "ok";
            }
        }
        else
        {
            health = "failed";
        }
        return health;
    }
}