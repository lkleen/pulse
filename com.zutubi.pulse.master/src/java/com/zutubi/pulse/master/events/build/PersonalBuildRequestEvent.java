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

package com.zutubi.pulse.master.events.build;

import com.zutubi.pulse.core.model.NamedEntity;
import com.zutubi.pulse.master.model.*;
import com.zutubi.pulse.master.tove.config.project.ProjectConfiguration;

import java.io.File;

import static com.zutubi.pulse.core.dependency.ivy.IvyStatus.STATUS_INTEGRATION;

/**
 * A request for a personal build.
 */
public class PersonalBuildRequestEvent extends BuildRequestEvent
{
    private long number;
    private User user;
    private File patch;
    private String patchFormat;

    public PersonalBuildRequestEvent(Object source, long number, BuildRevision revision, User user, File patch, String patchFormat, ProjectConfiguration projectConfig, TriggerOptions options)
    {
        super(source, revision, projectConfig, options);
        this.number = number;
        this.user = user;
        this.patch = patch;
        this.patchFormat = patchFormat;
    }

    public NamedEntity getOwner()
    {
        return user;
    }

    public boolean isPersonal()
    {
        return true;
    }

    public BuildResult createResult(ProjectManager projectManager, BuildManager buildManager)
    {
        Project project = projectManager.getProject(getProjectConfig().getProjectId(), false);
        BuildResult result = new BuildResult(options.getReason(), user, project, number);
        // although a personal build doesn't have it's ivy file published, it still
        // requires a status.
        result.setStatus(getStatus());
        result.setMetaBuildId(getMetaBuildId());

        buildManager.save(result);

        return result;
    }

    public String getStatus()
    {
        return STATUS_INTEGRATION;
    }

    public File getPatch()
    {
        return patch;
    }

    public String getPatchFormat()
    {
        return patchFormat;
    }

    public long getNumber()
    {
        return number;
    }

    public User getUser()
    {
        return user;
    }

    public String toString()
    {
        String result = "Personal Build Request Event: " + number;
        if(user != null)
        {
            result += ": " + user.getLogin();
        }
        return result;
    }
}
