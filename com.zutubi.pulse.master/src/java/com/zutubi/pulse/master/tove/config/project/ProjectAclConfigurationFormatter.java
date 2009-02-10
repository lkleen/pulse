package com.zutubi.pulse.master.tove.config.project;

import com.zutubi.pulse.master.tove.config.group.AbstractGroupConfiguration;

/**
 */
public class ProjectAclConfigurationFormatter
{
    public String getGroup(ProjectAclConfiguration configuration)
    {
        AbstractGroupConfiguration group = configuration.getGroup();
        return group == null ? null : group.getName();
    }

    public String getAllowedActions(ProjectAclConfiguration configuration)
    {
        return configuration.getAllowedActions().toString();
    }
}