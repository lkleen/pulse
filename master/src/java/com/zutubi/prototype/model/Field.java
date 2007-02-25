package com.zutubi.prototype.model;

/**
 *
 *
 */
public class Field extends UIComponent
{
    public Field()
    {
    }
    
    public Field(String name, String label, String type, Object value)
    {
        setName(name);
        setLabel(label);
        setType(type);
        setValue(value);
    }

    public int getTabindex()
    {
        return (Integer)parameters.get("tabindex");
    }

    public Field setTabindex(int tabindex)
    {
        parameters.put("tabindex", tabindex);
        return this;
    }

    public Field setName(String name)
    {
        parameters.put("name", name);
        return this;
    }

    public String getName()
    {
        return (String) parameters.get("name");
    }

    public Field setType(String type)
    {
        parameters.put("type", type);
        return this;
    }

    public String getType()
    {
        return (String) parameters.get("type");
    }

    public Field setLabel(String label)
    {
        parameters.put("label", label);
        return this;
    }

    public String getLabel()
    {
        return (String)parameters.get("label");
    }

    public Field setValue(Object value)
    {
        parameters.put("value", value);
        return this;
    }

    public Object getValue()
    {
        return parameters.get("value");
    }

    public Field setId(String id)
    {
        parameters.put("id", id);
        return this;
    }

    public String getId()
    {
        return (String) parameters.get("id");
    }
}
