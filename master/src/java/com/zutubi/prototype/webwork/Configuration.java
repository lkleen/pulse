package com.zutubi.prototype.webwork;

import com.opensymphony.util.TextUtils;
import com.zutubi.prototype.config.ConfigurationPersistenceManager;
import com.zutubi.prototype.type.CollectionType;
import com.zutubi.prototype.type.CompositeType;
import com.zutubi.prototype.type.ListType;
import com.zutubi.prototype.type.MapType;
import com.zutubi.prototype.type.PrimitiveType;
import com.zutubi.prototype.type.Type;
import com.zutubi.prototype.type.TypeRegistry;
import com.zutubi.prototype.type.TypeException;
import com.zutubi.prototype.type.record.RecordManager;
import com.zutubi.prototype.type.record.Record;
import com.zutubi.prototype.type.record.PathUtils;
import com.zutubi.prototype.annotation.ConfigurationCheck;
import com.zutubi.pulse.bootstrap.ComponentContext;
import com.zutubi.pulse.util.StringUtils;
import com.zutubi.pulse.prototype.config.ConfigurationExtension;

import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

/**
 *
 *
 */
public class Configuration
{
    private RecordManager recordManager;
    private ConfigurationPersistenceManager configurationPersistenceManager;

    private TypeRegistry typeRegistry;

    private Record record;
    private Type type;
    private String typeSymbolicName;

    private Type targetType;
    private String targetSymbolicName;

    private String path;
    private String[] pathElements;
    private String parentPath;
    private String[] parentPathElements;
    private String currentPath;
    private List<String> simpleProperties = new LinkedList<String>();
    private List<String> nestedProperties = new LinkedList<String>();
    private List<String> listProperties = new LinkedList<String>();
    private List<String> mapProperties = new LinkedList<String>();
    private List<String> extensions = new LinkedList<String>();

    private boolean configurationCheckAvailable = false;
    private Type checkType;

    public Configuration(String path)
    {
        if (!TextUtils.stringSet(path))
        {
            throw new IllegalArgumentException("Path must be provided.");
        }

        this.path = normalizePath(path);

        ComponentContext.autowire(this);
    }

    private String normalizePath(String path)
    {
        if (path.startsWith("/"))
        {
            path = path.substring(1);
        }
        if (path.endsWith("/"))
        {
            path = path.substring(0, path.length() -1);
        }
        return path;
    }

    public void analyse()
    {
        // load the type defined by the path.
        pathElements = PathUtils.getPathElements(path);
        if (pathElements.length == 0)
        {
            return;
        }
        
        parentPathElements = PathUtils.getParentPathElements(pathElements);
        parentPath = PathUtils.getPath(parentPathElements);
        currentPath = pathElements[pathElements.length - 1];

        record = recordManager.load(path);

        parentPath = configurationPersistenceManager.getParentPath(path);

        type = configurationPersistenceManager.getType(path);

        typeSymbolicName = type.getSymbolicName();

        targetType = configurationPersistenceManager.getTargetType(type);
        targetSymbolicName = targetType.getSymbolicName();

        if (targetType instanceof CompositeType)
        {
            CompositeType ctype = (CompositeType) targetType;
            if (!ConfigurationExtension.class.isAssignableFrom(targetType.getClazz()))
            {
                // only show a simple properties form if it is not associated with an extension type.
                for (String propertyName : ctype.getPropertyNames(PrimitiveType.class))
                {
                    simpleProperties.add(propertyName);
                }
            }
            for (String propertyName : ctype.getPropertyNames(CompositeType.class))
            {
                nestedProperties.add(propertyName);
            }
            for (String propertyName : ctype.getPropertyNames(ListType.class))
            {
                listProperties.add(propertyName);
            }
            for (String propertyName : ctype.getPropertyNames(MapType.class))
            {
                mapProperties.add(propertyName);
            }

            extensions.addAll(((CompositeType)targetType).getExtensions());

            // where should this happen? maybe it is something that the typeRegistry should be able to handle...
            // via additional processors..? post processors? .. maybe split the processing into propertyProcessors and
            // annotation processors ... or something similar..
            ConfigurationCheck annotation = (ConfigurationCheck) targetType.getAnnotation(ConfigurationCheck.class);
            if (annotation != null)
            {
                try
                {
                    Class checkClass = annotation.value();
                    // ensure that a type is available
                    checkType = typeRegistry.getType(checkClass);
                    if (checkType == null)
                    {
                        this.checkType = typeRegistry.register(checkClass);
                    }
                }
                catch (TypeException e)
                {
                    e.printStackTrace();
                }
            }
        }

        configurationCheckAvailable = checkType != null;
    }

    public boolean isConfigurationCheckAvailable()
    {
        return configurationCheckAvailable;
    }

    public Type getCheckType()
    {
        return checkType;
    }

    public String getTypeSymbolicName()
    {
        return typeSymbolicName;
    }

    public String getPath()
    {
        return path;
    }

    public String getParentPath()
    {
        return parentPath;
    }

    public String getCurrentPath()
    {
        return currentPath;
    }

    public String[] getParentPathElements()
    {
        return parentPathElements;
    }

    public String[] getPathElements()
    {
        return pathElements;
    }

    public Record getRecord()
    {
        return record;
    }

    public Type getType()
    {
        return type;
    }

    public Type getTargetType()
    {
        return targetType;
    }

    public List<String> getSimpleProperties()
    {
        return simpleProperties;
    }

    public List<String> getNestedProperties()
    {
        return nestedProperties;
    }

    public List<String> getListProperties()
    {
        return listProperties;
    }

    public List<String> getMapProperties()
    {
        return mapProperties;
    }

    public List<String> getExtensions()
    {
        return extensions;
    }

    public void setRecordManager(RecordManager recordManager)
    {
        this.recordManager = recordManager;
    }

    public String getTargetSymbolicName()
    {
        return targetSymbolicName;
    }

    public void setConfigurationPersistenceManager(ConfigurationPersistenceManager configurationPersistenceManager)
    {
        this.configurationPersistenceManager = configurationPersistenceManager;
    }

    public void setTypeRegistry(TypeRegistry typeRegistry)
    {
        this.typeRegistry = typeRegistry;
    }
}
