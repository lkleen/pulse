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

package com.zutubi.pulse.master.tove.config.project.changeviewer;

import com.zutubi.pulse.core.scm.api.FileChange;
import com.zutubi.pulse.core.scm.api.Revision;
import com.zutubi.pulse.core.scm.api.ScmException;
import com.zutubi.pulse.core.scm.config.api.ScmConfiguration;
import com.zutubi.pulse.master.tove.config.project.ProjectConfiguration;
import com.zutubi.tove.annotations.Form;
import com.zutubi.tove.annotations.SymbolicName;
import com.zutubi.util.StringUtils;
import com.zutubi.util.WebUtils;

/**
 * A change viewer for linking to ViewVC.
 */
@Form(fieldOrder = {"baseURL", "projectPath"})
@SymbolicName("zutubi.viewVCChangeViewerConfig")
public class ViewVCChangeViewer extends BasePathChangeViewer
{
    private static final String CVS_TYPE = "cvs";

    public ViewVCChangeViewer()
    {
        super(null, null);
    }

    public ViewVCChangeViewer(String baseURL, String projectPath)
    {
        super(baseURL, projectPath);
    }

    public boolean hasCapability(Capability capability)
    {
        ScmConfiguration scm = lookupScmConfiguration();
        if (capability.equals(Capability.VIEW_REVISION) && scm.getType().equals(CVS_TYPE))
        {
            return false;
        }
        
        return super.hasCapability(capability);
    }

    public String getRevisionURL(ProjectConfiguration projectConfiguration, Revision revision)
    {
        return StringUtils.join("/", true, true, getBaseURL(), getProjectPath() + "?rev=" + revision.getRevisionString() + "&view=rev");
    }

    public String getFileViewURL(ChangeContext context, FileChange fileChange)
    {
        return StringUtils.join("/", true, true, getBaseURL(), getProjectPath(), pathPart(fileChange) + "?rev=" + fileChange.getRevision() + "&view=markup");
    }

    public String getFileDownloadURL(ChangeContext context, FileChange fileChange)
    {
        return StringUtils.join("/", true, true, getBaseURL(), "*checkout*", getProjectPath(), pathPart(fileChange) + "?rev=" + fileChange.getRevision());
    }

    public String getFileDiffURL(ChangeContext context, FileChange fileChange) throws ScmException
    {
        Revision previous = context.getPreviousFileRevision(fileChange);
        if(previous == null)
        {
            return null;
        }

        return StringUtils.join("/", true, true, getBaseURL(), getProjectPath(), pathPart(fileChange) + "?r1=" + previous + "&r2=" + fileChange.getRevision());
    }

    private String pathPart(FileChange fileChange)
    {
        return WebUtils.uriPathEncode(fileChange.getPath());
    }
}
