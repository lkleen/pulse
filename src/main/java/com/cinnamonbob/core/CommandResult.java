package com.cinnamonbob.core;

/**
 * Describes the result of executing a command.
 */
public interface CommandResult
{    
    /**
     * True iff the command execution was successful.
     * 
     * @return true iff the command succeeded
     */
    public boolean succeeded();
    
    /**
     * Returns a succint version of the commands output.
     * 
     * @return a summary of the command output
     */
    public String getSummary();

    /**
     * @see BuildResult#changedBy(String)
     */
    public boolean changedBy(String login);
}
