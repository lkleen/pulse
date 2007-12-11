package com.zutubi.pulse.core;

import com.zutubi.util.TextUtils;
import com.zutubi.pulse.core.config.Resource;
import com.zutubi.pulse.core.config.ResourceVersion;

/**
 * A resource references adds the named resource to the current scope.
 *
 * Sample usage:
 *
 *     <resource name="ant" version="1.6.5"/>
 *
 */
public class ResourceReference implements ResourceAware, ScopeAware, InitComponent
{
    /**
     * The name of the referenced resource.
     */
    private String name;
    /**
     * The version of the referenced resource.
     */
    private String version;

    /**
     * Indicates whether or not the reference resource is required.
     */
    private boolean required = true;

    private ResourceRepository repository;
    private Scope scope;

    public void setScope(PulseScope scope)
    {
        this.scope = scope;
    }

    public void initBeforeChildren() throws FileLoadException
    {
        Resource resource = null;

        if (repository != null)
        {
            resource = repository.getResource(name);
        }

        if (resource == null)
        {
            if (required)
            {
                throw new FileLoadException("Reference to undefined resource '" + name + "'");
            }
            return;
        }

        // FIXME scope
        ((PulseScope)scope).getParent().add(resource.getProperties().values());
        String importVersion = version;
        if(importVersion == null)
        {
            importVersion = resource.getDefaultVersion();
        }

        if (TextUtils.stringSet(importVersion))
        {
            ResourceVersion resourceVersion = resource.getVersion(importVersion);

            if (resourceVersion == null)
            {
                if (required)
                {
                    throw new FileLoadException("Reference to undefined version '" + importVersion + "' of resource '" + name + "'");
                }
                return;
            }

            // FIXME scope
            ((PulseScope)scope).getParent().add(resourceVersion.getProperties().values());
        }
    }

    public void initAfterChildren()
    {
        // Nothing to do
    }

    public void setResourceRepository(ResourceRepository repo)
    {
        this.repository = repo;
    }

    public String getName()
    {
        return name;
    }

    /**
     * The name of the resource being referenced.
     *
     * This parameter is required.
     *
     * @param name
     */
    public void setName(String name)
    {
        this.name = name;
    }

    public String getVersion()
    {
        return version;
    }

    /**
     * The version of the resource being referenced.
     *
     * This parameter is optional
     *
     * @param version
     */
    public void setVersion(String version)
    {
        this.version = version;
    }

    public boolean isRequired()
    {
        return required;
    }

    /**
     * Indicate whether or not this resource referenced must be resolved. If set to true
     * and the resource is not located, an exception will be thrown during the build process.
     *
     * This parameter is optional, defaults to false.
     *
     * @param required
     */
    public void setRequired(boolean required)
    {
        this.required = required;
    }

}
