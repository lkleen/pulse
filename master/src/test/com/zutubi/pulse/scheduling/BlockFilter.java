package com.zutubi.pulse.scheduling;

import com.zutubi.pulse.events.Event;

/**
 */
public class BlockFilter implements EventTriggerFilter
{
    public boolean accept(Trigger trigger, Event event, TaskExecutionContext context)
    {
        return false;
    }

    public boolean dependsOnProject(Trigger trigger, long projectId)
    {
        return false;
    }
}
