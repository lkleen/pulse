package com.zutubi.pulse.master.tove.config.agent;

import com.zutubi.pulse.master.tove.config.group.AbstractGroupConfiguration;
import com.zutubi.tove.annotations.ItemPicker;
import com.zutubi.tove.annotations.Reference;
import com.zutubi.tove.annotations.SymbolicName;
import com.zutubi.tove.annotations.Table;
import com.zutubi.tove.config.api.AbstractConfiguration;
import com.zutubi.validation.annotations.Required;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents the authority to perform some action on some agent.
 */
@SymbolicName("zutubi.agentAclConfig")
@Table(columns = {"group", "allowedActions"})
public class AgentAclConfiguration extends AbstractConfiguration
{
    @Reference
    @Required
    private AbstractGroupConfiguration group;
    @ItemPicker(optionProvider = "AgentAuthorityProvider")
    private List<String> allowedActions = new LinkedList<String>();

    public AgentAclConfiguration()
    {
    }

    public AgentAclConfiguration(AbstractGroupConfiguration group, String... actions)
    {
        this.group = group;
        this.allowedActions.addAll(Arrays.asList(actions));
    }

    public AbstractGroupConfiguration getGroup()
    {
        return group;
    }

    public void setGroup(AbstractGroupConfiguration group)
    {
        this.group = group;
    }

    public List<String> getAllowedActions()
    {
        return allowedActions;
    }

    public void setAllowedActions(List<String> allowedActions)
    {
        this.allowedActions = allowedActions;
    }
}