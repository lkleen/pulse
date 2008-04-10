package com.zutubi.prototype.config.events;

import com.zutubi.prototype.config.ConfigurationTemplateManager;
import com.zutubi.pulse.core.config.Configuration;

/**
 * Raised when an instance has been inserted and the transaction is being
 * committed.  Note that you should not interact with the configuration
 * system while handling this event.  If you need to interact with the
 * configuration system, consider handling {@link InsertEvent}.
 */
public class PostInsertEvent extends CascadableEvent
{
    public PostInsertEvent(ConfigurationTemplateManager source, Configuration newInstance, boolean cascaded)
    {
        super(source, newInstance, cascaded);
    }

    public boolean isPost()
    {
        return true;
    }

    public String toString()
    {
        return "Post Insert Event: " + getInstance().getConfigurationPath();
    }
}
