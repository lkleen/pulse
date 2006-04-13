/********************************************************************************
 @COPYRIGHT@
 ********************************************************************************/
package com.zutubi.pulse.filesystem;

/**
 * <class-comment/>
 */
public class FileNotFoundException extends FileSystemException
{
    public FileNotFoundException(String errorMessage)
    {
        super(errorMessage);
    }

    public FileNotFoundException()
    {
    }

    public FileNotFoundException(Throwable cause)
    {
        super(cause);
    }

    public FileNotFoundException(String errorMessage, Throwable cause)
    {
        super(errorMessage, cause);
    }
}
