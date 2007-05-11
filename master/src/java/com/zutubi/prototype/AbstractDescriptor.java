package com.zutubi.prototype;

import java.util.HashMap;
import java.util.Map;

/**
 *
 *
 */
public abstract class AbstractDescriptor implements Descriptor
{
    protected Map<String, Object> parameters = new HashMap<String, Object>();

    public void addParameter(String key, Object value)
    {
        parameters.put(key, value);
    }

    public void addAll(Map<String, Object> parameters)
    {
        this.parameters.putAll(parameters);
    }

    public Map<String, Object> getParameters()
    {
        return this.parameters;
    }

    public boolean hasParameter(String key)
    {
        return parameters.containsKey(key);
    }

    public Object getParameter(String key)
    {
        return this.parameters.get(key);
    }

    public Object getParameter(String key, Object defaultValue)
    {
        Object value = parameters.get(key);
        if(value == null)
        {
            value = defaultValue;
        }
        return value;
    }
}
