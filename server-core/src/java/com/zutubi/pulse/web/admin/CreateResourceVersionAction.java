package com.zutubi.pulse.web.admin;

import com.zutubi.pulse.core.model.Resource;
import com.zutubi.pulse.core.model.ResourceVersion;
import com.zutubi.pulse.model.persistence.ResourceDao;
import com.zutubi.pulse.web.ActionSupport;

/**
 * Used to add a new version to a resource.
 */
public class CreateResourceVersionAction extends ActionSupport
{
    private long resourceId;
    private Resource resource;
    private ResourceVersion resourceVersion = new ResourceVersion();
    private ResourceDao resourceDao;

    public long getResourceId()
    {
        return resourceId;
    }

    public void setResourceId(long resourceId)
    {
        this.resourceId = resourceId;
    }

    public Resource getResource()
    {
        return resource;
    }

    public ResourceVersion getResourceVersion()
    {
        return resourceVersion;
    }

    public void setResourceVersion(ResourceVersion resourceVersion)
    {
        this.resourceVersion = resourceVersion;
    }

    public String doInput()
    {
        resource = resourceDao.findById(resourceId);
        return INPUT;
    }

    public void validate()
    {
        if (hasErrors())
        {
            // do not attempt to validate unless all other validation rules have 
            // completed successfully.
            return;
        }

        resource = resourceDao.findById(resourceId);
        if (resource == null)
        {
            addActionError("Unknown resource [" + resourceId + "]");
            return;
        }

        if (resource.hasVersion(resourceVersion.getValue()))
        {
            addFieldError("resourceVersion.value", "this resource already has a version '" + resourceVersion.getValue() + "'");
        }
    }

    public String execute()
    {
        resource.add(resourceVersion);
        resourceDao.save(resource);

        return SUCCESS;
    }

    public void setResourceDao(ResourceDao resourceDao)
    {
        this.resourceDao = resourceDao;
    }
}
