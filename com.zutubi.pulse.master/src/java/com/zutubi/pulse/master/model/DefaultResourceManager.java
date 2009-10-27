package com.zutubi.pulse.master.model;

import com.zutubi.events.Event;
import com.zutubi.events.EventManager;
import com.zutubi.pulse.core.ResourceRepository;
import com.zutubi.pulse.core.config.ResourceConfiguration;
import com.zutubi.pulse.core.config.ResourceVersionConfiguration;
import com.zutubi.pulse.master.agent.Agent;
import com.zutubi.pulse.master.agent.HostLocationFormatter;
import com.zutubi.pulse.master.tove.config.MasterConfigurationRegistry;
import com.zutubi.pulse.master.tove.config.agent.AgentConfiguration;
import com.zutubi.pulse.master.tove.config.project.ResourceRequirementConfiguration;
import com.zutubi.tove.config.ConfigurationProvider;
import com.zutubi.tove.config.ConfigurationTemplateManager;
import com.zutubi.tove.config.TypeAdapter;
import com.zutubi.tove.config.TypeListener;
import com.zutubi.tove.events.ConfigurationEventSystemStartedEvent;
import com.zutubi.tove.events.ConfigurationSystemStartedEvent;
import com.zutubi.tove.type.record.PathUtils;
import com.zutubi.tove.type.record.TemplateRecord;
import com.zutubi.util.CollectionUtils;
import com.zutubi.util.NullaryFunction;
import com.zutubi.util.Predicate;

import java.util.*;

/**
 */
public class DefaultResourceManager implements ResourceManager, com.zutubi.events.EventListener
{
    private Map<Long, AgentResourceRepository> agentRepositories = new TreeMap<Long, AgentResourceRepository>();
    private Map<Long, ResourceConfiguration> resourcesByHandle = new HashMap<Long, ResourceConfiguration>();
    private Map<Long, ResourceVersionConfiguration> resourceVersionsByHandle = new HashMap<Long, ResourceVersionConfiguration>();

    private ConfigurationProvider configurationProvider;
    private ConfigurationTemplateManager configurationTemplateManager;

    private void registerConfigListeners(ConfigurationProvider configurationProvider)
    {
        this.configurationProvider = configurationProvider;
        TypeListener<AgentConfiguration> agentListener = new TypeAdapter<AgentConfiguration>(AgentConfiguration.class)
        {
            public void postInsert(AgentConfiguration instance)
            {
                addAgentRepo(instance);
            }

            public void postDelete(AgentConfiguration instance)
            {
                agentRepositories.remove(instance.getHandle());
            }

            public void postSave(AgentConfiguration instance, boolean nested)
            {
                // Replaces the existing as it is stored by the (unchanging)
                // handle.
                addAgentRepo(instance);
            }
        };
        agentListener.register(configurationProvider, true);

        TypeListener<ResourceConfiguration> resourceListener = new TypeAdapter<ResourceConfiguration>(ResourceConfiguration.class)
        {
            public void save(ResourceConfiguration instance, boolean nested)
            {
                if (!nested)
                {
                    updateResource(instance);
                }
            }

            public void postInsert(ResourceConfiguration instance)
            {
                addResource(instance);
            }

            public void postDelete(ResourceConfiguration instance)
            {
                removeResource(instance);
            }

            public void postSave(ResourceConfiguration instance, boolean nested)
            {
                // Replaces the existing as it is stored by the (unchanging)
                // handle.
                addResource(instance);
            }
        };
        resourceListener.register(configurationProvider, true);

        TypeListener<ResourceVersionConfiguration> resourceVersionListener = new TypeAdapter<ResourceVersionConfiguration>(ResourceVersionConfiguration.class)
        {
            public void save(ResourceVersionConfiguration instance, boolean nested)
            {
                if (!nested)
                {
                    updateResourceVersion(instance);
                }
            }

            public void postInsert(ResourceVersionConfiguration instance)
            {
                addResourceVersion(instance);
            }

            public void postDelete(ResourceVersionConfiguration instance)
            {
                removeResourceVersion(instance);
            }

            public void postSave(ResourceVersionConfiguration instance, boolean nested)
            {
                // Replaces the existing as it is stored by the (unchanging)
                // handle.
                addResourceVersion(instance);
            }
        };
        resourceVersionListener.register(configurationProvider, true);
    }

    public void init()
    {
        for (ResourceConfiguration resource : configurationProvider.getAll(ResourceConfiguration.class))
        {
            addResource(resource);
        }

        for (ResourceVersionConfiguration resourceVersion : configurationProvider.getAll(ResourceVersionConfiguration.class))
        {
            addResourceVersion(resourceVersion);
        }

        for (AgentConfiguration agentConfig : configurationProvider.getAll(AgentConfiguration.class))
        {
            addAgentRepo(agentConfig);
        }
    }

    private void addResource(ResourceConfiguration resource)
    {
        resourcesByHandle.put(resource.getHandle(), resource);
    }

    private void removeResource(ResourceConfiguration resource)
    {
        resourcesByHandle.remove(resource.getHandle());
    }

    private void updateResource(ResourceConfiguration resource)
    {
        ResourceConfiguration oldResource = resourcesByHandle.get(resource.getHandle());
        if (oldResource != null)
        {
            String oldName = oldResource.getName();
            String newName = resource.getName();
            if (!oldName.equals(newName))
            {
                for (ResourceRequirementConfiguration requirement : configurationProvider.getAll(ResourceRequirementConfiguration.class))
                {
                    if (requirement.getResource().equals(oldName))
                    {
                        ResourceRequirementConfiguration clone = configurationProvider.deepClone(requirement);
                        clone.setResource(newName);
                        configurationProvider.save(clone);
                    }
                }
            }
        }
    }

    private void addResourceVersion(ResourceVersionConfiguration resourceVersion)
    {
        resourceVersionsByHandle.put(resourceVersion.getHandle(), resourceVersion);
    }

    private void removeResourceVersion(ResourceVersionConfiguration resourceVersion)
    {
        resourceVersionsByHandle.remove(resourceVersion.getHandle());
    }

    private void updateResourceVersion(ResourceVersionConfiguration resourceVersion)
    {
        ResourceVersionConfiguration oldVersion = resourceVersionsByHandle.get(resourceVersion.getHandle());
        if (oldVersion != null)
        {
            String oldValue = oldVersion.getValue();
            String newValue = resourceVersion.getValue();
            if (!oldValue.equals(newValue))
            {
                ResourceConfiguration owningResource = configurationProvider.getAncestorOfType(resourceVersion, ResourceConfiguration.class);
                String resourceName = owningResource.getName();
                for (ResourceRequirementConfiguration requirement : configurationProvider.getAll(ResourceRequirementConfiguration.class))
                {
                    if (requirement.getResource().equals(resourceName) && requirement.getVersion().equals(oldValue))
                    {
                        ResourceRequirementConfiguration clone = configurationProvider.deepClone(requirement);
                        clone.setVersion(newValue);
                        configurationProvider.save(clone);
                    }
                }
            }
        }
    }

    private void addAgentRepo(AgentConfiguration agentConfiguration)
    {
        agentRepositories.put(agentConfiguration.getHandle(), new AgentResourceRepository(agentConfiguration));
    }

    public ResourceRepository getAgentRepository(long handle)
    {
        return agentRepositories.get(handle);
    }

    public ResourceRepository getAgentRepository(AgentConfiguration agent)
    {
        return agentRepositories.get(agent.getHandle());
    }

    public ResourceRepository getAgentRepository(Agent agent)
    {
        return getAgentRepository(agent.getConfig());
    }

    public List<AgentConfiguration> addDiscoveredResources(final String hostLocation, final List<ResourceConfiguration> discoveredResources)
    {
        // Go direct to the config system.  We don't want to mess with our
        // cache here at all, because:
        //   - it may be out of date (if an event is pending); and
        //   - it will be invalidated by this change and updated by our event
        //     handler anyway
        return configurationProvider.executeInsideTransaction(new NullaryFunction<List<AgentConfiguration>>()
        {
            public List<AgentConfiguration> process()
            {
                List<AgentConfiguration> agentConfigs = getAgentsThatDefineLocation(hostLocation);
                List<AgentConfiguration> affectedAgents = new LinkedList<AgentConfiguration>();
                for (AgentConfiguration config: agentConfigs)
                {
                    String agentPath = config.getConfigurationPath();
                    for (ResourceConfiguration discoveredResource : discoveredResources)
                    {
                        // Check that no descendent defines the resource, so we
                        // don't conflict with those definitions.
                        if (noDescendentDefinesResource(agentPath, discoveredResource.getName()))
                        {
                            Map<String, ResourceConfiguration> agentResources = config.getResources();
                            addResource(agentPath, discoveredResource, agentResources.get(discoveredResource.getName()));

                            // Lookup again, we just changed this agent.
                            config = configurationProvider.get(agentPath, AgentConfiguration.class);
                        }
                    }

                    affectedAgents.add(config);
                }

                return affectedAgents;
            }
        });
    }

    private List<AgentConfiguration> getAgentsThatDefineLocation(final String hostLocation)
    {
        Collection<AgentConfiguration> allAgents = configurationTemplateManager.getAllInstances(PathUtils.getPath(MasterConfigurationRegistry.AGENTS_SCOPE, PathUtils.WILDCARD_ANY_ELEMENT), AgentConfiguration.class, true);
        return CollectionUtils.filter(allAgents, new Predicate<AgentConfiguration>()
        {
            public boolean satisfied(AgentConfiguration configuration)
            {
                return definesLocation(configuration, hostLocation);
            }
        });
    }

    private boolean definesLocation(AgentConfiguration config, String hostLocation)
    {
        if (locationMatches(config, hostLocation))
        {
            TemplateRecord templateParentRecord = configurationTemplateManager.getTemplateParentRecord(config.getConfigurationPath());
            if (templateParentRecord != null)
            {
                AgentConfiguration templateParentConfig = configurationTemplateManager.getInstance(templateParentRecord.getHandle(), AgentConfiguration.class);
                if (!locationMatches(templateParentConfig, hostLocation))
                {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean locationMatches(AgentConfiguration config, String hostLocation)
    {
        return HostLocationFormatter.format(config).equals(hostLocation);
    }

    private boolean noDescendentDefinesResource(String agentPath, String resourceName)
    {
        Set<AgentConfiguration> descendents = configurationProvider.getAllDescendents(agentPath, AgentConfiguration.class, true, false);
        for (AgentConfiguration descendent: descendents)
        {
            if (descendent.getResources().containsKey(resourceName))
            {
                return false;
            }
        }

        return true;
    }

    private void addResource(String agentPath, ResourceConfiguration discoveredResource, ResourceConfiguration existingResource)
    {
        if (existingResource == null)
        {
            configurationProvider.insert(PathUtils.getPath(agentPath, "resources"), discoveredResource);
        }
        else
        {
            existingResource = configurationProvider.deepClone(existingResource);

            // we have an existing resource, so merge the details.
            for (String propertyName: discoveredResource.getProperties().keySet())
            {
                if (!existingResource.hasProperty(propertyName))
                {
                    existingResource.addProperty(discoveredResource.getProperty(propertyName));
                }
            }

            for (String versionStr : discoveredResource.getVersions().keySet())
            {
                if (!existingResource.hasVersion(versionStr))
                {
                    existingResource.add(discoveredResource.getVersion(versionStr));
                }
                else
                {
                    ResourceVersionConfiguration version = discoveredResource.getVersion(versionStr);
                    ResourceVersionConfiguration existingVersion = existingResource.getVersion(versionStr);

                    for (String propertyName: version.getProperties().keySet())
                    {
                        if (!existingVersion.hasProperty(propertyName))
                        {
                            existingVersion.addProperty(version.getProperty(propertyName));
                        }
                    }
                }
            }

            configurationProvider.save(existingResource);
        }
    }

    public Map<String, List<ResourceConfiguration>> findAll()
    {
        Map<String, List<ResourceConfiguration>> allResources = new HashMap<String, List<ResourceConfiguration>>();
        for (AgentResourceRepository repo : agentRepositories.values())
        {
            allResources.put(repo.getAgentConfig().getName(), new LinkedList<ResourceConfiguration>(repo.getAll().values()));
        }

        return allResources;
    }

    public void handleEvent(Event event)
    {
        if (event instanceof ConfigurationEventSystemStartedEvent)
        {
            registerConfigListeners(((ConfigurationEventSystemStartedEvent)event).getConfigurationProvider());
        }
        else
        {
            init();
        }
    }

    public Class[] getHandledEvents()
    {
        return new Class[]{ ConfigurationEventSystemStartedEvent.class, ConfigurationSystemStartedEvent.class };
    }

    public void setEventManager(EventManager eventManager)
    {
        eventManager.register(this);
    }

    public void setConfigurationTemplateManager(ConfigurationTemplateManager configurationTemplateManager)
    {
        this.configurationTemplateManager = configurationTemplateManager;
    }
}
