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

package com.zutubi.pulse.master.vfs.provider.pulse.scm;

import com.zutubi.pulse.core.scm.api.ScmFile;
import com.zutubi.pulse.master.scm.ScmManager;
import com.zutubi.pulse.master.vfs.provider.pulse.FileAction;
import com.zutubi.pulse.master.vfs.provider.pulse.ProjectProvider;
import com.zutubi.util.WebUtils;
import com.zutubi.util.logging.Logger;
import org.apache.commons.vfs.FileName;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileType;
import org.apache.commons.vfs.provider.AbstractFileSystem;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The ScmFileObject represents a node in the file structure that respresents a
 * remote ScmFile object.
 */
public class ScmFileObject extends AbstractScmFileObject
{
    private static final Logger LOG = Logger.getLogger(ScmFileObject.class);

    private List<FileAction> availableActions = Collections.emptyList();

    private ScmFile scmFile;

    public ScmFileObject(ScmFile scmFile, FileName name, AbstractFileSystem fs)
    {
        super(name, fs);
        this.scmFile = scmFile;
    }

    public void doAttach()
    {
        if (scmFile.isFile())
        {
            try
            {
                ProjectProvider projectProvider = getAncestor(ProjectProvider.class);
                if (projectProvider != null)
                {
                    long id = projectProvider.getProjectId();
                    availableActions = Arrays.asList(new FileAction(FileAction.TYPE_DOWNLOAD, "/downloadSCMFile.action?projectId=" + id + "&path=" + WebUtils.formUrlEncode(scmFile.getPath())));
                }
            }
            catch (FileSystemException e)
            {
                LOG.severe(e);
            }
        }
    }

    public void doDetach()
    {
        synchronized(this)
        {
            availableActions = Collections.emptyList();
            
            loadedChildren = null;

            // if we are refreshing, we are going to make the assumption that
            // our parent will refresh us if necessary just like we are refreshing
            // our children.  This is not exactly ideal, however given the limitation
            // of the ScmClient.browse interface only returning a listing of children,
            // this will have to do for now.
        }
    }

    protected FileType doGetType() throws Exception
    {
        return scmFile.isDirectory() ? FileType.FOLDER : FileType.FILE;
    }

    protected String getScmPath()
    {
        return scmFile.getPath();
    }

    public List<FileAction> getActions()
    {
        return availableActions;
    }

    public void setScmManager(ScmManager scmManager)
    {
        this.scmManager = scmManager;
    }
}
