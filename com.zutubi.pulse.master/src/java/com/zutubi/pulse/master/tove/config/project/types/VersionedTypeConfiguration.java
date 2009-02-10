package com.zutubi.pulse.master.tove.config.project.types;

import com.zutubi.pulse.core.personal.PatchArchive;
import com.zutubi.pulse.core.scm.api.Revision;
import com.zutubi.pulse.core.scm.api.ScmClient;
import com.zutubi.pulse.core.scm.api.ScmContext;
import com.zutubi.pulse.core.scm.config.api.ScmConfiguration;
import com.zutubi.pulse.master.scm.ScmManager;
import com.zutubi.pulse.master.tove.config.project.ProjectConfiguration;
import com.zutubi.tove.annotations.FieldAction;
import com.zutubi.tove.annotations.SymbolicName;
import com.zutubi.tove.annotations.Transient;
import com.zutubi.tove.annotations.Wire;
import com.zutubi.util.FileSystemUtils;
import com.zutubi.util.io.IOUtils;
import com.zutubi.validation.annotations.Required;

import java.io.InputStream;

/**
 * Pulse file project where the pulse file is stored in the project's SCM.
 */
@SymbolicName("zutubi.versionedTypeConfig")
@Wire
public class VersionedTypeConfiguration extends TypeConfiguration
{
    @Required
    @FieldAction(template = "actions/browse-scm-file")
    private String pulseFileName;
    @Transient
    private ScmManager scmManager;

    public String getPulseFileName()
    {
        return pulseFileName;
    }

    public void setPulseFileName(String pulseFileName)
    {
        this.pulseFileName = pulseFileName;
    }

    public String getPulseFile(ProjectConfiguration projectConfig, Revision revision, PatchArchive patch) throws Exception
    {
        String normalisedPath = FileSystemUtils.normaliseSeparators(pulseFileName);
        if (patch == null || !patch.containsPath(normalisedPath))
        {
            ScmClient scmClient = null;
            InputStream is = null;
            try
            {
                ScmConfiguration scm = projectConfig.getScm();
                ScmContext context = scmManager.createContext(projectConfig);
                scmClient = scmManager.createClient(scm);
                is = scmClient.retrieve(context, pulseFileName, revision);
                return IOUtils.inputStreamToString(is);
            }
            finally
            {
                IOUtils.close(scmClient);
                IOUtils.close(is);
            }
        }
        else
        {
            return patch.retrieveFile(normalisedPath);
        }
    }

    public void setScmManager(ScmManager scmManager)
    {
        this.scmManager = scmManager;
    }
}