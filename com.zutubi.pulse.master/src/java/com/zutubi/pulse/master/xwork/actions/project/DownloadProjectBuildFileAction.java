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

package com.zutubi.pulse.master.xwork.actions.project;

import com.zutubi.pulse.core.engine.api.BuildException;
import com.zutubi.pulse.core.scm.api.Revision;
import com.zutubi.pulse.core.util.api.XMLUtils;
import com.zutubi.pulse.master.model.Project;
import com.zutubi.pulse.master.scm.ScmFileResolver;
import com.zutubi.pulse.master.scm.ScmManager;
import org.hsqldb.lib.StringInputStream;

import java.io.InputStream;

/**
 * An action to download the current pulse file for a project.
 */
public class DownloadProjectBuildFileAction extends ProjectActionBase
{
    private InputStream inputStream;
    private long contentLength;
    private ScmManager scmManager;

    public InputStream getInputStream()
    {
        return inputStream;
    }

    public String getContentType()
    {
        return "application/xml";
    }

    public long getContentLength()
    {
        return contentLength;
    }

    public String execute() throws Exception
    {
        Project project = getRequiredProject();
        try
        {
            ScmFileResolver resolver = new ScmFileResolver(project, Revision.HEAD, scmManager);
            String pulseFile = XMLUtils.prettyPrint(project.getConfig().getType().getPulseFile().getFileContent(resolver));
            inputStream = new StringInputStream(pulseFile);
            contentLength = pulseFile.length();
        }
        catch(BuildException e)
        {
            addActionError(e.getMessage());
            return ERROR;
        }

        return SUCCESS;
    }

    public void setScmManager(ScmManager scmManager)
    {
        this.scmManager = scmManager;
    }
}
