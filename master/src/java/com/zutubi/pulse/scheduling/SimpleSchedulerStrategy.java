package com.zutubi.pulse.scheduling;

/**
 * Startegy for scheduling simple triggers, by creating Quartz SimpleTrigger
 * instances.
 */
public class SimpleSchedulerStrategy extends QuartzSchedulerStrategy
{
    public String canHandle()
    {
        return SimpleTrigger.TYPE;
    }

    protected org.quartz.Trigger createTrigger(Trigger trigger) throws SchedulingException
    {
        SimpleTrigger simpleTrigger = (SimpleTrigger) trigger;
        return new org.quartz.SimpleTrigger(Long.toString(simpleTrigger.getId()),
                QUARTZ_GROUP,
                simpleTrigger.getStartTime(),
                null,
                simpleTrigger.getRepeatCount(),
                simpleTrigger.getInterval());
    }
}