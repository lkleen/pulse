package com.zutubi.pulse.model;

import com.zutubi.pulse.core.model.FileRevision;
import com.zutubi.pulse.core.model.Revision;
import com.zutubi.pulse.util.StringUtils;

/**
 * A ChangeViewer for linking to a P4Web instance.
 */
public class P4WebChangeViewer extends BasePathChangeViewer
{
    private P4WebChangeViewer()
    {
        super(null, null);
    }

    public P4WebChangeViewer(String baseURL, String projectPath)
    {
        super(baseURL, projectPath);
    }

    public String getDetails()
    {
        return "P4Web [" + getBaseURL() + "]";
    }

    public String getChangesetURL(Revision revision)
    {
        return StringUtils.join("/", true, true, getBaseURL(), "@md=d@", revision.getRevisionString() + "?ac=10");
    }

    public String getFileViewURL(String path, FileRevision revision)
    {
        return StringUtils.join("/", true, true, getBaseURL(), "@md=d@" + path + "?ac=64&rev1=" + revision.getRevisionString());
    }

    public String getFileDownloadURL(String path, FileRevision revision)
    {
        return StringUtils.join("/", true, true, getBaseURL(), "@md=d&rev1=" + revision.getRevisionString() + "@" + path);
    }

    public String getFileDiffURL(String path, FileRevision revision)
    {
        FileRevision previousRevision = revision.getPrevious();
        if(previousRevision == null)
        {
            return null;
        }

        return StringUtils.join("/", true, true, getBaseURL(), "@md=d@" + path + "?ac=19&rev1=" + previousRevision.getRevisionString() + "&rev2=" + revision.getRevisionString());
    }

    public ChangeViewer copy()
    {
        return new P4WebChangeViewer(getBaseURL(), getProjectPath());
    }
}
