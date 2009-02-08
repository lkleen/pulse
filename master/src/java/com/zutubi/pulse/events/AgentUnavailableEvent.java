package com.zutubi.pulse.events;

import com.zutubi.pulse.agent.Agent;

/**
 * Raised when an agent becomes unavailable for builds.
 */
public class AgentUnavailableEvent extends AgentAvailabilityEvent
{
    public AgentUnavailableEvent(Object source, Agent agent)
    {
        super(source, agent);
    }

    public String toString()
    {
        return ("Agent Unavailable Event: " + getAgent().getName());
    }
}