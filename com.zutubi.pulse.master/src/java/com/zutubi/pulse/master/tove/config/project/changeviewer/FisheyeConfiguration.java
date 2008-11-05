package com.zutubi.pulse.master.tove.config.project.changeviewer;

import com.zutubi.pulse.core.scm.api.Revision;
import com.zutubi.pulse.core.scm.config.api.ScmConfiguration;
import static com.zutubi.pulse.master.tove.config.project.changeviewer.ChangeViewerUtils.*;
import com.zutubi.tove.annotations.Form;
import com.zutubi.tove.annotations.SymbolicName;
import com.zutubi.util.StringUtils;
import com.zutubi.util.TextUtils;

import java.util.Date;
import java.util.Map;

/**
 * A ChangeViewer for linking to a Fisheye instance.
 */
@Form(fieldOrder = {"baseURL", "projectPath", "pathStripPrefix"})
@SymbolicName("zutubi.fisheyeChangeViewerConfig")
public class FisheyeConfiguration extends BasePathChangeViewer
{
    static final String TYPE_CVS = "cvs";

    /**
     * Useful when configured against Perforce.  In this case the paths in
     * Pulse are full depot paths, but Fisheye expects a shorter form:
     * starting after a path configured in Fisheye itself.  This will be at
     * least //depot, but could be deeper, and needs to be stripped from the
     * front of all paths before using them to construct URLs.
     */
    private String pathStripPrefix;

    public FisheyeConfiguration()
    {
        super(null, null);
    }

    public FisheyeConfiguration(String baseURL, String projectPath)
    {
        super(baseURL, projectPath);
    }

    public String getPathStripPrefix()
    {
        return pathStripPrefix;
    }

    public void setPathStripPrefix(String pathStripPrefix)
    {
        this.pathStripPrefix = pathStripPrefix;
    }

    public String getChangelistURL(Revision revision)
    {
        return StringUtils.join("/", true, true, getBaseURL(), "changelog", getProjectPath(), "?cs=" + getChangesetString(revision));
    }

    public String getFileViewURL(String path, Revision changelistRevision, String fileRevision)
    {
        ScmConfiguration scm = lookupScmConfiguration();
        String revision = chooseRevision(scm, changelistRevision, fileRevision);
        return StringUtils.join("/", true, true, getBaseURL(), "browse", getProjectPath(), StringUtils.urlEncodePath(stripPathPrefix(path)) + "?r=" + revision);
    }

    public String getFileDownloadURL(String path, Revision changelistRevision, String fileRevision)
    {
        ScmConfiguration scm = lookupScmConfiguration();
        String revision = chooseRevision(scm, changelistRevision, fileRevision);
        return StringUtils.join("/", true, true, getBaseURL(), "browse", "~raw,r=" + revision, getProjectPath(), StringUtils.urlEncodePath(stripPathPrefix(path)));
    }

    public String getFileDiffURL(String path, Revision changelistRevision, String fileRevision)
    {
        ScmConfiguration scm = lookupScmConfiguration();
        String revision = chooseRevision(scm, changelistRevision, fileRevision);
        String previousRevision = scm.getPreviousRevision(revision);
        if(previousRevision == null)
        {
            return null;
        }

        return StringUtils.join("/", true, true, getBaseURL(), "browse", getProjectPath(), StringUtils.urlEncodePath(stripPathPrefix(path)) + "?r1=" + previousRevision + "&r2=" + revision);
    }

    private String chooseRevision(ScmConfiguration scm, Revision changelistRevision, String fileRevision)
    {
        if (isCVS(scm))
        {
            return fileRevision;
        }
        else
        {
            return changelistRevision.getRevisionString();
        }
    }

    private String stripPathPrefix(String path)
    {
        if(TextUtils.stringSet(pathStripPrefix) && path.startsWith(pathStripPrefix))
        {
            path = path.substring(pathStripPrefix.length());
        }

        return path;
    }

    private String getChangesetString(Revision revision)
    {
        ScmConfiguration scm = lookupScmConfiguration();
        if (isCVS(scm))
        {
            Map<String, Object> properties = getRevisionProperties(revision);
            if (properties.containsKey(PROPERTY_AUTHOR) &&
                properties.containsKey(PROPERTY_BRANCH) &&
                properties.containsKey(PROPERTY_DATE))
            {
                return String.format("%s:%s:%s", properties.get(PROPERTY_BRANCH), properties.get(PROPERTY_AUTHOR), CustomChangeViewerConfiguration.FISHEYE_DATE_FORMAT.format((Date) properties.get(PROPERTY_DATE)));
            }
        }

        return revision.getRevisionString();
    }

    private boolean isCVS(ScmConfiguration scm)
    {
        return scm.getType().equals(TYPE_CVS);
    }
}
