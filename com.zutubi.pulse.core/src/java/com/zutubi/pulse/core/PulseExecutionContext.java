package com.zutubi.pulse.core;

import static com.zutubi.pulse.core.engine.api.BuildProperties.NAMESPACE_INTERNAL;
import static com.zutubi.pulse.core.engine.api.BuildProperties.NAMESPACE_USER;
import com.zutubi.pulse.core.engine.api.ExecutionContext;
import com.zutubi.pulse.core.engine.api.Reference;
import com.zutubi.pulse.core.engine.api.ResourceProperty;
import com.zutubi.util.StringUtils;

import java.io.File;
import java.io.OutputStream;
import java.util.List;

/**
 * An environment in which commands are executed.  Consists of scopes,
 * working directory and output sink.
 *
 * The scopes are split into internal and external.  This gives us two
 * separate namespaces so that we can ensure user-defined properties do
 * not conflict with Pulse-internal properties.
 */
public class PulseExecutionContext implements ExecutionContext
{
    // If you add a field, remember to update the copy constructor
    private MultiScopeStack scopeStack;
    /**
     * The base directory for the build.
     */
    private File workingDir = null;
    /**
     * The output stream tied to this execution context.  All build output should be sent
     * via this output stream so that it can be captured and reported to the user.
     */
    private OutputStream outputStream = null;

    // from the build - used by maven to specify the maven version number of the build.  We should
    // pass this value around as a user(?) property rather than directly implemented as a field.
    private String version = null;

    public PulseExecutionContext()
    {
        scopeStack = new MultiScopeStack(NAMESPACE_INTERNAL, NAMESPACE_USER);
    }

    public PulseExecutionContext(PulseExecutionContext other)
    {
        this.scopeStack = new MultiScopeStack(other.scopeStack);
        this.workingDir = other.workingDir;
        this.outputStream = other.outputStream;
        this.version = other.version;
    }

    public String getString(String namespace, String name)
    {
        return getValue(namespace, name, String.class);
    }

    public String getString(String name)
    {
        return getValue(name, String.class);
    }

    public boolean getBoolean(String namespace, String name, boolean defaultValue)
    {
        return asBoolean(getValue(namespace, name, Object.class), defaultValue);
    }

    public boolean getBoolean(String name, boolean defaultValue)
    {
        return asBoolean(getValue(name, Object.class), defaultValue);
    }

    private boolean asBoolean(Object value, boolean defaultValue)
    {
        if (value != null)
        {
            if (value instanceof Boolean)
            {
                return (Boolean) value;
            }
            else if (value instanceof String)
            {
                return Boolean.parseBoolean((String) value);
            }
        }

        return defaultValue;
    }

    public long getLong(String namespace, String name, long defaultValue)
    {
        return asLong(getValue(namespace, name, Object.class), defaultValue);
    }

    public long getLong(String name, long defaultValue)
    {
        return asLong(getValue(name, Object.class), defaultValue);
    }

    private long asLong(Object value, long defaultValue)
    {
        if (value != null)
        {
            if (value instanceof Long)
            {
                return (Long) value;
            }
            else if (value instanceof String)
            {
                try
                {
                    return Long.parseLong((String) value);
                }
                catch (NumberFormatException e)
                {
                    // Fall through
                }
            }
        }

        return defaultValue;
    }

    public File getFile(String namespace, String name)
    {
        return asFile(getValue(namespace, name, Object.class));
    }

    public File getFile(String name)
    {
        return asFile(getValue(name, Object.class));
    }

    private File asFile(Object value)
    {
        if (value != null)
        {
            if (value instanceof File)
            {
                return (File) value;
            }
            else if (value instanceof String)
            {
                return new File((String) value);
            }
        }

        return null;
    }

    public <T> T getValue(String namespace, String name, Class<T> type)
    {
        return scopeStack.getScope(namespace).getReferenceValue(name, type);
    }

    public <T> T getValue(String name, Class<T> type)
    {
        return scopeStack.getScope().getReferenceValue(name, type);
    }

    public void add(String namespace, Reference reference)
    {
        scopeStack.getScope(namespace).add(reference);
    }

    public void add(Reference reference)
    {
        scopeStack.getScope().add(reference);
    }

    public void add(String namespace, ResourceProperty resourceProperty)
    {
        scopeStack.getScope(namespace).add(resourceProperty);
    }

    public void add(ResourceProperty resourceProperty)
    {
        scopeStack.getScope().add(resourceProperty);
    }

    public void addString(String namespace, String name, String value)
    {
        scopeStack.getScope(namespace).add(new GenericReference<String>(name, value));
    }

    public void addString(String name, String value)
    {
        scopeStack.getScope().add(new GenericReference<String>(name, value));
    }

    public void addValue(String namespace, String name, Object value)
    {
        scopeStack.getScope(namespace).add(new GenericReference<Object>(name, value));
    }

    public void addValue(String name, Object value)
    {
        scopeStack.getScope().add(new GenericReference<Object>(name, value));
    }

    public String resolveReferences(String input)
    {
        try
        {
            return ReferenceResolver.resolveReferences(input, getScope(), ReferenceResolver.ResolutionStrategy.RESOLVE_NON_STRICT);
        }
        catch (ResolutionException e)
        {
            // Never happens, but return unresolved anyway.
            return input;
        }
    }

    public List<String> splitAndResolveReferences(String input)
    {
        try
        {
            return ReferenceResolver.splitAndResolveReferences(input, getScope(), ReferenceResolver.ResolutionStrategy.RESOLVE_NON_STRICT);
        }
        catch (ResolutionException e)
        {
            // Never happens, just return split anyway.
            return StringUtils.split(input);
        }
    }

    /**
     * Get the root level scope associated with this execution context.  For ease of use,
     * numerous convenience methods have been added to this interface that provide access
     * to the contents of the scope through the Execution Context interface.
     *
     * @return the root level scope of this execution context.
     */
    public PulseScope getScope()
    {
        return scopeStack.getScope();
    }

    public void push()
    {
        scopeStack.push();
    }

    public void pop()
    {
        scopeStack.pop();
    }

    public void popTo(String label)
    {
        scopeStack.popTo(label);
    }

    public void setLabel(String label)
    {
        scopeStack.setLabel(label);
    }
    
    public File getWorkingDir()
    {
        return workingDir;
    }

    public void setWorkingDir(File workingDir)
    {
        this.workingDir = workingDir;
    }

    public OutputStream getOutputStream()
    {
        return outputStream;
    }

    public void setOutputStream(OutputStream outputStream)
    {
        this.outputStream = outputStream;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }
}