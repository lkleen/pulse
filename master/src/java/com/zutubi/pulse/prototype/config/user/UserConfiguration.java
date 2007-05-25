package com.zutubi.pulse.prototype.config.user;

import com.zutubi.prototype.type.Extendable;
import com.zutubi.pulse.core.config.AbstractNamedConfiguration;
import com.zutubi.pulse.prototype.config.user.contacts.ContactConfiguration;
import com.zutubi.config.annotations.Transient;
import com.zutubi.config.annotations.SymbolicName;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;

/**
 *
 *
 */
@SymbolicName("internal.userConfig")
public class UserConfiguration extends AbstractNamedConfiguration implements Extendable
{
    @Transient
    private Map<String, Object> extensions;

    private String login; // internal.

    private String name;

    private UserSettingsConfiguration settings = new UserSettingsConfiguration();

    private List<UserAliasConfiguration> alias = new LinkedList<UserAliasConfiguration>();

    private Map<String, ContactConfiguration> contacts = new HashMap<String, ContactConfiguration>();

    private List<SubscriptionConfiguration> subscriptions = new LinkedList<SubscriptionConfiguration>();

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getLogin()
    {
        return login;
    }

    public void setLogin(String login)
    {
        this.login = login;
    }

    public UserSettingsConfiguration getSettings()
    {
        return settings;
    }

    public void setSettings(UserSettingsConfiguration settings)
    {
        this.settings = settings;
    }

    public List<UserAliasConfiguration> getAlias()
    {
        return alias;
    }

    public void setAlias(List<UserAliasConfiguration> alias)
    {
        this.alias = alias;
    }

    public Map<String, ContactConfiguration> getContacts()
    {
        return contacts;
    }

    public void setContacts(Map<String, ContactConfiguration> contacts)
    {
        this.contacts = contacts;
    }

    public List<SubscriptionConfiguration> getSubscriptions()
    {
        return subscriptions;
    }

    public void setSubscriptions(List<SubscriptionConfiguration> subscriptions)
    {
        this.subscriptions = subscriptions;
    }

    public Map<String, Object> getExtensions()
    {
        if (extensions == null)
        {
            extensions = new HashMap<String, Object>();
        }
        return extensions;
    }
}
