/* Copyright 2017 Zutubi Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zutubi.pulse.master.tove.config.project.triggers;

import com.zutubi.events.Event;
import com.zutubi.events.EventListener;
import com.zutubi.events.EventManager;
import com.zutubi.pulse.core.api.PulseRuntimeException;
import com.zutubi.pulse.master.scheduling.*;
import com.zutubi.pulse.master.scheduling.tasks.BuildProjectTask;
import com.zutubi.pulse.master.tove.config.ConfigurationInjector;
import com.zutubi.pulse.master.tove.config.project.ProjectConfiguration;
import com.zutubi.tove.config.*;
import com.zutubi.tove.events.ConfigurationEventSystemStartedEvent;
import com.zutubi.util.logging.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the relationship between trigger configuration and scheduler
 * triggers.  Creates/syncs state, and wires config into it as required.
 */
public class TriggerManager implements ExternalStateManager<TriggerConfiguration>, ConfigurationInjector.ConfigurationSetter<Trigger>, EventListener
{
    private static final Logger LOG = Logger.getLogger(TriggerManager.class);

    private Map<Long, TriggerConfiguration> idToConfig = new ConcurrentHashMap<Long, TriggerConfiguration>();

    private ConfigurationProvider configurationProvider;
    private Scheduler scheduler;

    public void registerConfigListeners(ConfigurationProvider configurationProvider)
    {
        TypeListener<TriggerConfiguration> listener = new TypeAdapter<TriggerConfiguration>(TriggerConfiguration.class)
        {
            @Override
            public void postInsert(TriggerConfiguration instance)
            {
                scheduler.getTrigger(instance.getTriggerId()).setConfig(instance);
                idToConfig.put(instance.getTriggerId(), instance);
            }

            public void postDelete(TriggerConfiguration instance)
            {
                idToConfig.remove(instance.getTriggerId());
                TriggerManager.this.delete(instance.getTriggerId());
            }

            public void postSave(TriggerConfiguration instance, boolean nested)
            {
                idToConfig.put(instance.getTriggerId(), instance);
                try
                {
                    Trigger trigger = scheduler.getTrigger(instance.getTriggerId());
                    scheduler.preUpdate(trigger);
                    instance.update(trigger);
                    scheduler.postUpdate(trigger);
                }
                catch (SchedulingException e)
                {
                    LOG.severe(e);
                }
            }
        };
        listener.register(configurationProvider, true);

        for (TriggerConfiguration triggerConfig: configurationProvider.getAll(TriggerConfiguration.class))
        {
            idToConfig.put(triggerConfig.getTriggerId(), triggerConfig);
        }
    }

    private void delete(long id)
    {
        try
        {
            Trigger trigger = scheduler.getTrigger(id);
            if (trigger != null && trigger.isScheduled())
            {
                scheduler.unschedule(trigger);
            }
        }
        catch (SchedulingException e)
        {
            LOG.severe(e);
        }
    }

    public long createState(TriggerConfiguration instance)
    {
        try
        {
            ProjectConfiguration project = configurationProvider.getAncestorOfType(instance, ProjectConfiguration.class);
            Trigger trigger = instance.newTrigger();
            trigger.setTaskClass(BuildProjectTask.class);
            trigger.setProject(project.getProjectId());
            trigger.setGroup("project:" + project.getProjectId());
            trigger.setConfig(instance);
            scheduler.schedule(trigger);
            return trigger.getId();
        }
        catch (SchedulingException e)
        {
            LOG.severe(e);
            throw new PulseRuntimeException(e);
        }
    }

    public void rollbackState(long id)
    {
        delete(id);
    }

    public Object getState(long id)
    {
        return scheduler.getTrigger(id);
    }

    public void handleEvent(Event event)
    {
        registerConfigListeners(((ConfigurationEventSystemStartedEvent)event).getConfigurationProvider());
    }

    public Class[] getHandledEvents()
    {
        return new Class[]{ ConfigurationEventSystemStartedEvent.class };
    }

    public void setConfiguration(Trigger state)
    {
        state.setConfig(idToConfig.get(state.getId()));
    }

    public void setConfigurationInjector(ConfigurationInjector configurationInjector)
    {
        configurationInjector.registerSetter(CronTrigger.class, this);
        configurationInjector.registerSetter(EventTrigger.class, this);
        configurationInjector.registerSetter(NoopTrigger.class, this);
    }

    public void setConfigurationProvider(ConfigurationProvider configurationProvider)
    {
        this.configurationProvider = configurationProvider;
    }

    public void setConfigurationStateManager(ConfigurationStateManager configurationStateManager)
    {
        configurationStateManager.register(TriggerConfiguration.class, this);
    }

    public void setEventManager(EventManager eventManager)
    {
        eventManager.register(this);
    }

    public void setScheduler(Scheduler scheduler)
    {
        this.scheduler = scheduler;
    }
}
