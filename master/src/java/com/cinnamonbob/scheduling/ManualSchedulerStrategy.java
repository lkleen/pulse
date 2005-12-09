package com.cinnamonbob.scheduling;

/**
 * <class-comment/>
 */
public class ManualSchedulerStrategy implements SchedulerStrategy
{
    private TriggerHandler triggerHandler;

    public boolean canHandle(Trigger trigger)
    {
        return trigger instanceof ManualTrigger;
    }

    public void pause(Trigger trigger) throws SchedulingException
    {
        // does it make sense to do this?
    }

    public void resume(Trigger trigger) throws SchedulingException
    {
        // does it make sense to do this?
    }

    public void schedule(Trigger trigger, Task task) throws SchedulingException
    {
        trigger.setState(TriggerState.ACTIVE);
        triggerHandler.trigger(trigger);
        trigger.setState(TriggerState.NONE);
    }

    public void unschedule(Trigger trigger) throws SchedulingException
    {
        // does it make sense to do this?
    }

    public void setTriggerHandler(TriggerHandler handler) throws SchedulingException
    {
        this.triggerHandler = handler;
    }
}
