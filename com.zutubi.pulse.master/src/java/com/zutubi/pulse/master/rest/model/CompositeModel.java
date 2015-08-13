package com.zutubi.pulse.master.rest.model;

import com.zutubi.pulse.master.rest.model.forms.FormModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Model representing composites.
 */
public class CompositeModel extends ConfigModel
{
    private Map<String, Object> properties;
    private FormModel form;
    private List<ActionModel> actions;

    public CompositeModel()
    {
        super("composite");
    }

    public CompositeModel(String handle, String key, String label)
    {
        super("composite", handle, key, label);
    }

    public Map<String, Object> getProperties()
    {
        return properties;
    }

    public void setProperties(Map<String, Object> properties)
    {
        this.properties = properties;
    }

    public FormModel getForm()
    {
        return form;
    }

    public void setForm(FormModel form)
    {
        this.form = form;
    }

    public List<ActionModel> getActions()
    {
        return actions;
    }

    public void addAction(ActionModel action)
    {
        if (actions == null)
        {
            actions = new ArrayList<>();
        }
        actions.add(action);
    }
}