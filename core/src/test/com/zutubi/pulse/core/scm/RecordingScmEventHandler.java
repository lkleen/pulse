package com.zutubi.pulse.core.scm;

import com.zutubi.pulse.core.model.Change;

import java.util.List;
import java.util.LinkedList;
import java.util.Collections;


/**
 *
 *
 */
public class RecordingScmEventHandler implements ScmEventHandler
{
    private List<Change> changes = new LinkedList<Change>();

    public void fileChanged(Change change)
    {
        changes.add(change);
    }

    public List<Change> getChanges()
    {
        return Collections.unmodifiableList(changes);
    }

    public void reset()
    {
        changes.clear();
    }

    public void status(String message)
    {

    }

    public void checkCancelled() throws ScmCancelledException
    {

    }
}
