package com.zutubi.pulse.services;

import com.zutubi.pulse.events.Event;
import com.zutubi.pulse.core.model.Resource;

import java.util.List;

/**
 */
public interface MasterService
{
    void pong();

    void upgradeStatus(String token, UpgradeStatus upgradeStatus);

    void handleEvent(String token, Event event) throws InvalidTokenException;

    Resource getResource(String token, long slaveId, String name) throws InvalidTokenException;
    List<String> getResourceNames(String token, long slaveId) throws InvalidTokenException;
}