package com.zutubi.pulse.master.agent;

import com.zutubi.events.Event;
import com.zutubi.events.EventListener;
import com.zutubi.events.EventManager;
import com.zutubi.pulse.core.events.SlaveCommandCompletedEvent;
import com.zutubi.pulse.core.events.SlaveCommandEvent;
import com.zutubi.pulse.core.events.SlaveCommandOutputEvent;
import com.zutubi.util.logging.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Allows the connection of output event streams from agents with {@link OutputStream}s on the
 * master.
 */
public class SlaveCommandListener implements EventListener
{
    private static final Logger LOG = Logger.getLogger(SlaveCommandListener.class);

    private long nextId = 1;
    private Map<Long, SlaveCommand> idToCommand = new HashMap<Long, SlaveCommand>();

    public synchronized SlaveCommand registerCommandWithStream(OutputStream stream)
    {
        long id = nextId++;
        SlaveCommand command = new SlaveCommand(id, stream);
        idToCommand.put(id, command);
        return command;
    }

    public synchronized SlaveCommand unregisterCommand(long id)
    {
        return idToCommand.remove(id);
    }

    public void handleEvent(Event event)
    {
        SlaveCommand command;
        synchronized (this)
        {
            command = idToCommand.get(((SlaveCommandEvent) event).getCommandId());
        }

        if (command != null)
        {
            if (event instanceof SlaveCommandOutputEvent)
            {
                SlaveCommandOutputEvent gev = (SlaveCommandOutputEvent) event;
                try
                {
                    OutputStream stream = command.getStream();
                    stream.write(gev.getData());
                }
                catch (IOException e)
                {
                    LOG.warning("Could not write output event to local stream: " + e.getMessage(), e);
                }
            }
            else if (event instanceof SlaveCommandCompletedEvent)
            {
                SlaveCommandCompletedEvent cev = (SlaveCommandCompletedEvent) event;
                if (cev.isSuccess())
                {
                    command.success();
                }
                else
                {
                    command.failure(cev.getMessage());
                }
            }
        }
    }

    public Class[] getHandledEvents()
    {
        return new Class[]{ SlaveCommandEvent.class };
    }

    public void setEventManager(EventManager eventManager)
    {
        eventManager.register(this);
    }
}
