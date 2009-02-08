package com.zutubi.pulse.util;

import com.zutubi.pulse.util.logging.Logger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;
import java.util.zip.ZipFile;

/**
 * A utility class for standard IO operations.
 *
 * @author Daniel Ostermeier
 */
public class IOUtils
{
    private static final Logger LOG = Logger.getLogger(IOUtils.class);

    public static Properties read(File f) throws IOException
    {
        return read(new FileInputStream(f));
    }

    public static Properties read(InputStream input) throws IOException
    {
        try
        {
            Properties properties = new Properties();
            properties.load(input);
            return properties;
        }
        finally
        {
            IOUtils.close(input);
        }
    }

    public static void write(Properties properties, File dest) throws IOException
    {
        FileOutputStream out = null;
        try
        {
            out = new FileOutputStream(dest);
            properties.store(out, "");
        }
        finally
        {
            IOUtils.close(out);
        }
    }

    public static void close(Closeable closeable)
    {
        try
        {
            if (closeable != null)
            {
                closeable.close();
            }
        }
        catch (IOException e)
        {
            LOG.finest(e);
        }
    }

    public static void close(ZipFile zipFile)
    {
        try
        {
            if (zipFile != null)
            {
                zipFile.close();
            }
        }
        catch (IOException e)
        {
            LOG.finest(e);
        }
    }

    public static void joinStreams(InputStream input, OutputStream output) throws IOException
    {
        byte[] buffer = new byte[1024];
        int n;

        while (!Thread.interrupted() && (n = input.read(buffer)) > 0)
        {
            output.write(buffer, 0, n);
        }
    }

    public static void joinStreams(InputStream input, OutputStream output, boolean close) throws IOException
    {
        joinStreams(input, output);
        if (close)
        {
            close(input);
            close(output);
        }
    }

    public static void joinReaderToWriter(Reader reader, Writer writer) throws IOException
    {
        char[] buffer = new char[1024];
        int n;

        while ((n = reader.read(buffer)) > 0)
        {
            writer.write(buffer, 0, n);
        }
    }

    public static void copyFile(File fromFile, File toFile) throws IOException
    {
        FileInputStream inStream = null;
        FileOutputStream outStream = null;

        try
        {
            inStream = new FileInputStream(fromFile);
            outStream = new FileOutputStream(toFile);
            joinStreams(inStream, outStream);
        }
        finally
        {
            close(inStream);
            close(outStream);
        }
    }

    public static String inputStreamToString(InputStream is) throws IOException
    {
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        IOUtils.joinStreams(is, os);
        return os.toString();
    }

    public static String fileToString(File file) throws IOException
    {
        FileInputStream is = null;
        try
        {
            is = new FileInputStream(file);
            return inputStreamToString(is);
        }
        finally
        {
            close(is);
        }
    }

    public static byte[] fileToBytes(File file) throws IOException
    {
        FileInputStream is = null;
        ByteArrayOutputStream os = null;

        try
        {
            is = new FileInputStream(file);
            os = new ByteArrayOutputStream((int) file.length());
            joinStreams(is, os);
            return os.toByteArray();
        }
        finally
        {
            close(is);
            close(os);
        }
    }

    public static void downloadFile(URL url, File destination) throws IOException
    {
        FileOutputStream fos = null;
        InputStream urlStream = null;

        try
        {
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            int code = urlConnection.getResponseCode();
            if(code != 200)
            {
                String error = "Host returned code " + Integer.toString(code);
                String message = urlConnection.getResponseMessage();

                if(message != null)
                {
                    error += ": " + message;
                }

                throw new IOException(error);
            }

            // take url connection input stream and write contents to file
            fos = new FileOutputStream(destination);
            urlStream = urlConnection.getInputStream();
            IOUtils.joinStreams(urlStream, fos);
        }
        finally
        {
            IOUtils.close(urlStream);
            IOUtils.close(fos);
        }

    }

    /**
     * Copy the contents of the template file to the destination file.  This copy will filter out any lines
     * that begin with '###'
     * 
     * @param template
     * @param destination
     *
     * @throws IOException if there is a problem copying the template.
     */
    public static void copyTemplate(File template, File destination) throws IOException
    {
        File parentFile = destination.getParentFile();
        if (!parentFile.isDirectory() && !parentFile.mkdirs())
        {
            throw new IOException("Unable to create parent directory '" + parentFile.getAbsolutePath() + "' for config file");
        }
        if (!destination.createNewFile())
        {
            throw new IOException("Unable to create config file '" + destination.getAbsolutePath() + "'");
        }

        BufferedReader reader = null;
        BufferedWriter writer = null;

        try
        {
            reader = new BufferedReader(new FileReader(template));
            writer = new BufferedWriter(new FileWriter(destination));

            String line;
            boolean doneSkipping = false;

            while((line = reader.readLine()) != null)
            {
                if(doneSkipping || !line.startsWith("###"))
                {
                    doneSkipping = true;
                    writer.write(line);
                    writer.write('\n');
                }
            }
        }
        finally
        {
            IOUtils.close(reader);
            IOUtils.close(writer);
        }
    }
}