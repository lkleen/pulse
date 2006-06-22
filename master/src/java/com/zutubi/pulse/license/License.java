package com.zutubi.pulse.license;

import com.zutubi.pulse.util.Constants;
import com.zutubi.pulse.util.ObjectUtils;

import java.util.Date;
import java.util.Calendar;

/**
 * The license contains the details associated with a license, including the
 * holder of the license and the expiry date if it exists.
 *
 *
 */
public class License
{
    private LicenseType type;
    private String holder;
    private Date expiryDate;

    public static final int UNDEFINED = -1;

    private int supportedProjects = UNDEFINED;

    private int supportedUsers = UNDEFINED;

    private int supportedAgents = UNDEFINED;

    public License(LicenseType type, String holder, Date expiry)
    {
        this.type = type;
        this.holder = holder;
        this.expiryDate = expiry;
    }

    public void setSupported(int agents, int projects, int users)
    {
        this.supportedAgents = agents;
        this.supportedProjects = projects;
        this.supportedUsers = users;
    }

    /**
     * Get the license holder string.
     *
     * @return a string representing the owner of this license.
     */
    public String getHolder()
    {
        return holder;
    }

    /**
     * Get the expiry date of this license.
     *
     * @return the license expiry date.
     */
    public Date getExpiryDate()
    {
        return expiryDate;
    }

    /**
     * Get the type of this license. For example, evaluation.
     *
     * @return the name string
     */
    public LicenseType getType()
    {
        return type;
    }

    /**
     * Get the number of projects supported by this license.
     *
     * @return the number of projects allowed by this license.
     */
    public int getSupportedProjects()
    {
        return supportedProjects;
    }

    /**
     * Get the number of users supported by this license.
     *
     * @return the number of projects allowed by this license.
     */
    public int getSupportedUsers()
    {
        return supportedUsers;
    }

    /**
     * Get the number of agents supported by this license.
     *
     * @return the number of agents allowed by this license.
     */
    public int getSupportedAgents()
    {
        return supportedAgents;
    }

    public boolean equals(Object o)
    {
        if (!(o instanceof License))
        {
            return false;
        }
        License other = (License) o;

        return ObjectUtils.equals(holder, other.holder) &&
                ObjectUtils.equals(expiryDate, other.expiryDate) &&
                ObjectUtils.equals(type, other.type) &&
                ObjectUtils.equals(supportedAgents, other.supportedAgents) &&
                ObjectUtils.equals(supportedUsers, other.supportedUsers) &&
                ObjectUtils.equals(supportedProjects, other.supportedProjects);
    }


    /**
     *
     * @return true if the license has expired, false otherwise.
     */
    public boolean isExpired()
    {
        if (expires())
        {
            return getDaysRemaining() == 0;
        }
        return false;
    }

    /**
     *
     * @return true if this license expires, false otherwise.
     */
    public boolean expires()
    {
        return getExpiryDate() != null;
    }

    /**
     * Return the number of days remaining before this license expires.
     *
     * @return the number in days before this license expires. It will return
     * 0 if this license has expired, and -1 if it never expires.
     */
    public int getDaysRemaining()
    {
        if (expires())
        {
            return calculateDaysRemaining(Calendar.getInstance().getTime(), getExpiryDate());
        }
        return -1;
    }

    /**
     * Calculate the number of days remaining before the license expires. If the now date
     * falls on the same day as the expiry date, then we indicate 1 day as remaining. Expiry
     * occurs at midnight.
     *
     */
    static int calculateDaysRemaining(Date now, Date expiry)
    {
        // Make this static to allow testing.

        // normalise the expiry date to midnight on the day of expiry.

        Calendar x = Calendar.getInstance();
        x.setTime(expiry);

        // add one day, and zero out the rest of the details. We expire
        // at midnight.
        x.add(Calendar.DAY_OF_YEAR, 1);
        x.set(Calendar.HOUR_OF_DAY, 0);
        x.set(Calendar.MINUTE, 0);
        x.set(Calendar.SECOND, 0);
        x.set(Calendar.MILLISECOND, 0);

        int daysRemaining = 0;

        long timeRemainingInMilliSeconds = x.getTimeInMillis() - now.getTime();
        if (timeRemainingInMilliSeconds > 0)
        {
            // part days count as 1.
            daysRemaining = (int)(timeRemainingInMilliSeconds / Constants.DAY);
            if (timeRemainingInMilliSeconds % Constants.DAY > 0)
            {
                daysRemaining++;
            }
        }

        return daysRemaining;
    }
}
