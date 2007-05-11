package com.zutubi.prototype.webwork;

import com.zutubi.prototype.config.ReferenceCleanupTask;
import com.zutubi.prototype.type.record.PathUtils;
import com.zutubi.validation.i18n.DefaultTextProvider;
import com.zutubi.validation.i18n.TextProvider;

/**
 * Action for deleting a record.  Also handles displaying confirmation when
 * necessary.
 */
public class DeleteAction extends PrototypeSupport
{
    private ReferenceCleanupTask task;
    private String parentPath;
    private ConfigurationPanel newPanel;

    public ReferenceCleanupTask getTask()
    {
        return task;
    }

    public String getParentPath()
    {
        return parentPath;
    }

    public ConfigurationPanel getNewPanel()
    {
        return newPanel;
    }

    public TextProvider getTextProvider()
    {
        return new DefaultTextProvider();
    }

    public String execute() throws Exception
    {
        parentPath = PathUtils.getParentPath(path);

        if(isConfirmSelected())
        {
            task = configurationPersistenceManager.getCleanupTasks(getPath());
            newPanel = new ConfigurationPanel("aconfig/confirm.vm");
            return "confirm";
        }
        else if (isDeleteSelected())
        {
            configurationPersistenceManager.delete(path);
            response = new ConfigurationResponse(PathUtils.getParentPath(path));
            response.addInvalidatedPath(response.getNewPath());
            path = response.getNewPath();
            return SUCCESS;
        }
        else if(isCancelSelected())
        {
            response = new ConfigurationResponse(PathUtils.getParentPath(path));
            path = response.getNewPath();
            return "cancel";
        }
        
        return ERROR;
    }
}
