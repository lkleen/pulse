package com.zutubi.pulse.core;

/**
 * Interface for types that are aware of the scope in which they are loaded.
 */
public interface ScopeAware
{
    public void setScope(PulseScope scope);
}
