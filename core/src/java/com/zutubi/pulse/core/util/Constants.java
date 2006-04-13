/********************************************************************************
 @COPYRIGHT@
 ********************************************************************************/
package com.zutubi.pulse.core.util;

/**
 * <class-comment/>
 */
public class Constants
{
    public static final long MEGABYTE = 1048576;

    public static final long SECOND = 1000;
    public static final long MINUTE = 60 * SECOND;
    public static final long HOUR = 60 * MINUTE;
    public static final long DAY = 24 * HOUR;
    public static final long WEEK = 7 * DAY;
    public static final long YEAR = 365 * DAY;

    // Line separator string.  This is the value of the line.separator
    // property at the moment that the StdOutErrReader was created.
    public static final String LINE_SEPARATOR = (String) java.security.AccessController.doPrivileged(new sun.security.action.GetPropertyAction("line.separator"));
}
