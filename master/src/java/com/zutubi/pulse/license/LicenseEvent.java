package com.zutubi.pulse.license;

import com.zutubi.pulse.events.Event;

/**
 * The base event type for license related events.
 */
public class LicenseEvent extends Event<License>
{
    public LicenseEvent(License source)
    {
        super(source);
    }

    /**
     * Get the license associated with this event.
     *
     * @return the license instance associated with this event.
     */
    public License getLicense()
    {
        return getSource();
    }

    public String toString()
    {
        return "License Event";
    }    
}