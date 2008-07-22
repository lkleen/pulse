package com.zutubi.pulse.events;

import com.zutubi.pulse.agent.Agent;

/**
 * Raised when an agent comes online.
 */
public class AgentAvailableEvent extends AgentAvailabilityEvent
{
    public AgentAvailableEvent(Object source, Agent agent)
    {
        super(source, agent);
    }

    public String toString()
    {
        return ("Agent Available Event: " + getAgent().getName());
    }
}
