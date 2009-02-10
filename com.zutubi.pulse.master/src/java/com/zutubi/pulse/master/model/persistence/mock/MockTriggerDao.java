package com.zutubi.pulse.master.model.persistence.mock;


import com.zutubi.pulse.master.model.persistence.TriggerDao;
import com.zutubi.pulse.master.scheduling.Trigger;

import java.util.List;

public class MockTriggerDao extends MockEntityDao<Trigger> implements TriggerDao
{
    public List<Trigger> findByGroup(final String group)
    {
        return findByFilter(new Filter<Trigger>()
        {
            public boolean include(Trigger trigger)
            {
                return group.compareTo(trigger.getGroup()) == 0;
            }
        });
    }

    public Trigger findByNameAndGroup(final String name, final String group)
    {
        return findUniqueByFilter(new Filter<Trigger>()
        {
            public boolean include(Trigger trigger)
            {
                return group.compareTo(trigger.getGroup()) == 0 &&
                        name.compareTo(trigger.getName()) == 0;
            }
        });
    }

    public List<Trigger> findByProject(final long id)
    {
        return findByFilter(new Filter<Trigger>()
        {
            public boolean include(Trigger trigger)
            {
                return trigger.getProject() == id;
            }
        });
    }

    public Trigger findByProjectAndName(final long id, final String name)
    {
        return findUniqueByFilter(new Filter<Trigger>()
        {
            public boolean include(Trigger trigger)
            {
                return name.compareTo(trigger.getName()) == 0 &&
                        id == trigger.getProject();
            }
        });
    }
}