/********************************************************************************
  @COPYRIGHT@
 ********************************************************************************/
package com.zutubi.pulse.events.build;

import com.zutubi.pulse.core.model.CommandResult;

/**
 */
public class CommandCompletedEvent extends RecipeEvent
{
    private CommandResult result;

    public CommandCompletedEvent(Object source, long recipeId, CommandResult result)
    {
        super(source, recipeId);
        this.result = result;
    }

    public CommandResult getResult()
    {
        return result;
    }
}
