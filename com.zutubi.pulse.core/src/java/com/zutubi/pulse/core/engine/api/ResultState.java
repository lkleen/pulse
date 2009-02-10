package com.zutubi.pulse.core.engine.api;

/**
 * The possible states for a build, stage or command result.  Indicates what
 * step of the lifecycle the result has reached, and in the case of a completed
 * result whether it succeeded.
 */
public enum ResultState
{
    /**
     * The result has not yet commenced.
     */
    INITIAL
            {
                public boolean isBroken()
                {
                    return false;
                }

                public boolean isCompleted()
                {
                    return false;
                }

                public String getString()
                {
                    return "pending";
                }

                public String getPrettyString()
                {
                    return "pending";
                }
            },
    /**
     * The result has commenced and is in progress (not yet completed).
     */
    IN_PROGRESS
            {
                public boolean isBroken()
                {
                    return false;
                }

                public boolean isCompleted()
                {
                    return false;
                }

                public String getString()
                {
                    return "inprogress";
                }

                public String getPrettyString()
                {
                    return "in progress";
                }
            },
    /**
     * The result has been asked forcefully to complete, and will do so as soon
     * as possible.
     */
    TERMINATING
            {
                public boolean isBroken()
                {
                    return false;
                }

                public boolean isCompleted()
                {
                    return false;
                }

                public String getString()
                {
                    return "terminating";
                }

                public String getPrettyString()
                {
                    return "terminating";
                }
            },
    /**
     * The result completed successfully.
     */
    SUCCESS
            {
                public boolean isBroken()
                {
                    return false;
                }

                public boolean isCompleted()
                {
                    return true;
                }

                public String getString()
                {
                    return "success";
                }

                public String getPrettyString()
                {
                    return "success";
                }
            },
    /**
     * The result has completed and has failed due to a build problem.
     */
    FAILURE
            {
                public boolean isBroken()
                {
                    return true;
                }

                public boolean isCompleted()
                {
                    return true;
                }

                public String getString()
                {
                    return "failure";
                }

                public String getPrettyString()
                {
                    return "failure";
                }
            },
    /**
     * The result has completed and has failed due to an external problem.
     */
    ERROR
            {
                public boolean isBroken()
                {
                    return true;
                }

                public boolean isCompleted()
                {
                    return true;
                }

                public String getString()
                {
                    return "error";
                }

                public String getPrettyString()
                {
                    return "error";
                }
            };

    private static final ResultState[] BROKEN_STATES;
    private static final ResultState[] COMPLETED_STATES;
    private static final ResultState[] INCOMPLETE_STATES;
    static
    {
        int broken = 0;
        int complete = 0;
        int incomplete = 0;
        for(ResultState state: values())
        {
            if(state.isBroken())
            {
                broken++;
            }

            if(state.isCompleted())
            {
                complete++;
            }
            else
            {
                incomplete++;
            }
        }

        BROKEN_STATES = new ResultState[broken];
        COMPLETED_STATES = new ResultState[complete];
        INCOMPLETE_STATES = new ResultState[incomplete];

        broken = 0;
        complete = 0;
        incomplete = 0;
        for(ResultState state: values())
        {
            if(state.isBroken())
            {
                BROKEN_STATES[broken++] = state;
            }

            if(state.isCompleted())
            {
                COMPLETED_STATES[complete++] = state;
            }
            else
            {
                INCOMPLETE_STATES[incomplete++] = state;
            }
        }
    }

    /**
     * @return the set of states that return true from {@link #isBroken()}
     */
    public static ResultState[] getBrokenStates()
    {
        return BROKEN_STATES;
    }
    
    /**
     * @return the set of states that return true from {@link #isCompleted()}
     */
    public static ResultState[] getCompletedStates()
    {
        return COMPLETED_STATES;
    }

    /**
     * @return the set of states that return false from {@link #isCompleted()}
     */
    public static ResultState[] getIncompleteStates()
    {
        return INCOMPLETE_STATES;
    }

    /**
     * Returns the more severe of two completed states.  Errors take precedence
     * over failures, and failures over success.
     *
     * @param s1 the first completed state
     * @param s2 the second completed state
     * @return the more severe of the two completed states
     */
    public static ResultState getWorseState(ResultState s1, ResultState s2)
    {
        if (s1 == ERROR || s2 == ERROR)
        {
            return ERROR;
        }
        else if (s1 == FAILURE || s2 == FAILURE)
        {
            return FAILURE;
        }
        else
        {
            return SUCCESS;
        }
    }

    /**
     * Parses a pretty string representation back into a state.
     *
     * @param prettyString the pretty string, as returned by {@link #getPrettyString()}
     * @return the state that has the given pretty string
     * @throws IllegalArgumentException if no state has the given pretty string
     */
    public static ResultState fromPrettyString(String prettyString)
    {
        for(ResultState state: values())
        {
            if(state.getPrettyString().equals(prettyString))
            {
                return state;
            }
        }

        throw new IllegalArgumentException("No such result state '" + prettyString + "'");
    }

    /**
     * @return true if this state indicates a result that is complete but
     *         unsuccessful
     */
    public abstract boolean isBroken();

    /**
     * @return true if this state indicates a result that is complete
     */
    public abstract boolean isCompleted();
    /**
     * @return a human-readable string for this state
     */
    public abstract String getPrettyString();
    /**
     * @return an all lower-case machine-readable string for this state
     */
    public abstract String getString();
}