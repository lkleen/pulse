package com.cinnamonbob.core.model;

import com.cinnamonbob.core.Reference;


/**
 * 
 *
 */
public class Property extends Entity implements Reference
{
    private String name;
    private String value;

    public Property()
    {

    }

    public Property(String name, String value)
    {
        this.name = name;
        this.value = value;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

}
