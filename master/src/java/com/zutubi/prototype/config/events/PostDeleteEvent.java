package com.zutubi.prototype.config.events;

import com.zutubi.prototype.config.ConfigurationTemplateManager;
import com.zutubi.pulse.core.config.Configuration;

/**
 * Raised when an instance has been deleted and the transaction is being
 * committed.  Note that you should not interact with the configuration
 * system while handling this event.  If you need to interact with the
 * configuration system, consider handling {@link DeleteEvent}.
 */
public class PostDeleteEvent extends CascadableEvent
{
    public PostDeleteEvent(ConfigurationTemplateManager source, Configuration oldInstance, boolean cascaded)
    {
        super(source, oldInstance, cascaded);
    }

    public boolean isPost()
    {
        return true;
    }

    public String toString()
    {
        return "Post Delete Event: " + getInstance().getConfigurationPath();
    }
}
