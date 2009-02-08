package com.zutubi.pulse.model;

/**
 * The build was triggered via the remote API.
 */
public class RemoteTriggerBuildReason extends AbstractBuildReason
{
    public boolean isUser()
    {
        return true;
    }

    public String getSummary()
    {
        return "trigger via remote api";
    }
}