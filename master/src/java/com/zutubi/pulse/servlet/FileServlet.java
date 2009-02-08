package com.zutubi.pulse.servlet;

import com.opensymphony.util.TextUtils;
import com.zutubi.pulse.bootstrap.ComponentContext;
import com.zutubi.pulse.util.IOUtils;
import com.zutubi.pulse.util.logging.Logger;
import com.zutubi.pulse.vfs.pulse.AbstractPulseFileObject;
import org.apache.commons.vfs.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLConnection;

/**
 * <class comment/>
 */
public class FileServlet extends HttpServlet
{
    private static final Logger LOG = Logger.getLogger(FileServlet.class);

    private FileSystemManager fsManager;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        try
        {
            String path = request.getPathInfo();

            path = path.replace("\\", "/");

            if (path.startsWith("/"))
            {
                path = path.substring(1);
            }

            path = "pulse:///" + path;

            AbstractPulseFileObject pfo = (AbstractPulseFileObject) getFS().resolveFile(path);

            // if the pfo is a file, download it. If it is a folder, list the directory.
            if (pfo.getType() == FileType.FILE)
            {
                doDownload(request, response, pfo);
            }
            else
            {
                doList(request, response, pfo);
            }
        }
        catch (IOException e)
        {
            LOG.error(e);
            throw e;
        }
        catch (Exception e)
        {
            throw new ServletException(e);
        }
    }

    private void doList(HttpServletRequest request, HttpServletResponse response, AbstractPulseFileObject pfo) throws IOException
    {
/*
        FileObject base = getFS().resolveFile("pulse:///");
        FileName baseName = base.getName();

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(response.getOutputStream()));
        writer.append("<html>");
        writer.append("<body>");
        writer.append("<ul>");
        for (FileObject fo : pfo.getChildren())
        {
            writer.append("<li>");
            writer.append("<a href='"+ request.getContextPath() + request.getServletPath() + "/"+ baseName.getRelativeName(fo.getName())  +"'>");
            writer.append(fo.getName().getBaseName());
            writer.append("</a>");
            writer.append("</li>");
        }
        writer.append("</ul>");
        writer.append("</body>");
        writer.append("</html>");

        writer.flush();
*/
        response.sendError(404, "Can not display requested resource '" + request.getPathInfo() + "', this resource it is not a file.");
    }

    private void doDownload(HttpServletRequest request, HttpServletResponse response, AbstractPulseFileObject pfo) throws IOException
    {
        String filename = pfo.getName().getBaseName();

        String contentType = pfo.getContent().getContentInfo().getContentType();
        if (TextUtils.stringSet(contentType))
        {
            response.setContentType(contentType);
        }
        else
        {
            response.setContentType(URLConnection.guessContentTypeFromName(filename));
        }

        response.setContentLength((int) pfo.getContent().getSize());

        InputStream is = null;
        try
        {
            is = pfo.getContent().getInputStream();
            IOUtils.joinStreams(is, response.getOutputStream());
        }
        finally
        {
            // ensure that we close the open file.
            IOUtils.close(is);
        }
    }

    protected FileSystemManager getFS() throws FileSystemException
    {
        if (fsManager == null)
        {
            fsManager = (FileSystemManager) ComponentContext.getBean("fileSystemManager");
        }
        return fsManager;
    }

    public void setFileSystemManager(FileSystemManager fsManager)
    {
        this.fsManager = fsManager;
    }
}