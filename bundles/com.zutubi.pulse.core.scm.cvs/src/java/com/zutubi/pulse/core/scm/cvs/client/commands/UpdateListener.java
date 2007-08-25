package com.zutubi.pulse.core.scm.cvs.client.commands;

import com.zutubi.pulse.core.model.Change;
import com.zutubi.pulse.core.scm.ScmEventHandler;
import com.zutubi.pulse.core.scm.ScmFile;
import com.zutubi.util.logging.Logger;
import org.netbeans.lib.cvsclient.command.DefaultFileInfoContainer;
import org.netbeans.lib.cvsclient.command.FileInfoContainer;
import org.netbeans.lib.cvsclient.event.CVSAdapter;
import org.netbeans.lib.cvsclient.event.FileInfoEvent;
import org.netbeans.lib.cvsclient.event.FileRemovedEvent;

import java.io.File;
import java.io.IOException;

/**
 * <class comment/>
 */
public class UpdateListener extends CVSAdapter
{
    private static final Logger LOG = Logger.getLogger(UpdateListener.class);

    private final ScmEventHandler handler;

    private final File workingDirectory;

    public UpdateListener(ScmEventHandler handler, File workingDirectory)
    {
        if (handler == null)
        {
            throw new IllegalArgumentException("handler is a required argument.");
        }
        if (workingDirectory == null)
        {
            throw new IllegalArgumentException("working directory is a required argument.");
        }
        this.handler = handler;
        this.workingDirectory = workingDirectory;
    }

    public void fileRemoved(FileRemovedEvent e)
    {
        handler.fileChanged(new Change(e.getFilePath(), null, Change.Action.DELETE));
    }

    public void fileInfoGenerated(FileInfoEvent evt)
    {
        FileInfoContainer c = evt.getInfoContainer();
        if (!(c instanceof DefaultFileInfoContainer))
        {
            return;
        }

        DefaultFileInfoContainer infoContainer = (DefaultFileInfoContainer) evt.getInfoContainer();
        if ("U".equals(infoContainer.getType()))
        {
            try
            {
                String path = relativePath(infoContainer.getFile());
                handler.fileChanged(new Change(path, null, Change.Action.EDIT));
            }
            catch (IOException e)
            {
                LOG.warning(e);
            }
        }
    }

    /**
     * Return the string that represents the path of the file relative to the working directory, with separators
     * normalized.
     *
     * @param file for which we want the relative path.
     *
     * @return the relative path
     *
     * @throws IOException in case of error.
     */
    private String relativePath(File file) throws IOException
    {
        String path = file.getCanonicalPath().substring(workingDirectory.getCanonicalPath().length());
        path = ScmFile.normalizePath(path);
        return path;
    }
}
