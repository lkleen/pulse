package com.zutubi.prototype.config;

import com.zutubi.prototype.type.*;
import com.zutubi.prototype.type.record.PathUtils;
import com.zutubi.prototype.type.record.Record;
import com.zutubi.prototype.type.record.RecordManager;
import com.zutubi.pulse.core.config.Configuration;

import java.util.*;

/**
 *
 *
 */
@SuppressWarnings({"unchecked"})
public class ConfigurationPersistenceManager
{
    private TypeRegistry typeRegistry;
    private RecordManager recordManager;

    private Map<String, ConfigurationScopeInfo> rootScopes = new HashMap<String, ConfigurationScopeInfo>();
    /**
     * An index mapping from composite types to all paths where records of that
     * type (or a subtype) may reside.  Paths will include wildcards to navigate
     * collection members.
     */
    private Map<CompositeType, List<String>> compositeTypePathIndex = new HashMap<CompositeType, List<String>>();

    public void register(String scope, ComplexType type)
    {
        register(scope, type, true);
    }

    /**
     * Register the root scope definitions, from which all of the other definitions will be
     * derived.
     *
     * @param scope      name of the scope
     * @param type       type of the object stored in this scope
     * @param persistent if true, records under this scope will be persisted
     */
    public void register(String scope, ComplexType type, boolean persistent)
    {
        validateConfiguration(type);
        rootScopes.put(scope, new ConfigurationScopeInfo(scope, type, persistent));
        if (persistent)
        {
            if(recordManager == null)
            {
                throw new IllegalArgumentException("Attempt to register persistent scope '" + scope + "' before persistence system is initialised");
            }

            if(!recordManager.containsRecord(scope))
            {
                recordManager.insert(scope, type.createNewRecord());
            }
        }

        if (type instanceof CompositeType)
        {
            updateIndex(scope, (CompositeType) type);
        }
        else
        {
            if (type instanceof CollectionType)
            {
                updateIndex(scope, (CollectionType) type);
            }
        }
    }

    public Collection<ConfigurationScopeInfo> getScopes()
    {
        return rootScopes.values();
    }

    public ConfigurationScopeInfo getScopeInfo(String scope)
    {
        return rootScopes.get(scope);
    }

    private void validateConfiguration(ComplexType type)
    {
        if (type instanceof CollectionType)
        {
            Type targetType = type.getTargetType();
            if (targetType instanceof ComplexType)
            {
                validateConfiguration((ComplexType) targetType);
            }
        }
        else
        {
            CompositeType compositeType = (CompositeType) type;
            if (!Configuration.class.isAssignableFrom(compositeType.getClazz()))
            {
                throw new IllegalArgumentException("Attempt to register persistent configuration of type '" + compositeType.getClazz() + "': which does not implement Configuration");
            }

            for (TypeProperty property : compositeType.getProperties(ComplexType.class))
            {
                validateConfiguration((ComplexType) property.getType());
            }
        }
    }

    private void updateIndex(String path, CompositeType type)
    {
        // Add an entry at the current path, and analyse properties
        addToIndex(type, path);
        for (TypeProperty property : type.getProperties(CompositeType.class))
        {
            String childPath = PathUtils.getPath(path, property.getName());
            updateIndex(childPath, (CompositeType) property.getType());
        }
        for (TypeProperty property : type.getProperties(CollectionType.class))
        {
            String childPath = PathUtils.getPath(path, property.getName());
            updateIndex(childPath, (CollectionType) property.getType());
        }
    }

    private void updateIndex(String path, CollectionType type)
    {
        // If the collection itself holds a complex type, add a wildcard
        // to the path and traverse down.
        Type targetType = type.getCollectionType();
        if (targetType instanceof CompositeType)
        {
            updateIndex(PathUtils.getPath(path, PathUtils.WILDCARD_ANY_ELEMENT), (CompositeType) targetType);
        }
    }

    private void addToIndex(CompositeType composite, String path)
    {
        ensureConfigurationPaths(composite).add(path);
    }

    private List<String> ensureConfigurationPaths(CompositeType type)
    {
        List<String> l = compositeTypePathIndex.get(type);
        if (l == null)
        {
            l = new ArrayList<String>();
            compositeTypePathIndex.put(type, l);
        }
        return l;
    }

/*
<<<<<<< .mine
    public Map<String, Record> getReferencableRecords(CompositeType type, String referencingPath)
    {
        HashMap<String, Record> records = new HashMap<String, Record>();
        // FIXME does not account for templating, and may need to be more
        // FIXME general.  review when we have more config objects...
        for (String path : getOwningPaths(type, getClosestOwningScope(type, referencingPath)))
        {
            recordManager.loadAll(path, records);
        }

        return records;
    }

    public <T> T getAncestorOfType(Configuration c, Class<T> clazz)
    {
        String path = c.getConfigurationPath();
        CompositeType type = typeRegistry.getType(clazz);
        if (type != null)
        {
            String ancestorPath = getClosestMatchingScope(type, path);
            if(ancestorPath != null)
            {
                return (T) getInstance(ancestorPath);
            }
        }

        return null;
    }

=======
>>>>>>> .r3386
*/
    String getClosestMatchingScope(CompositeType type, String path)
    {
        List<String> patterns = compositeTypePathIndex.get(type);
        if (patterns != null)
        {
            // Find the closest by starting at our path and working up the
            // ancestry until one hits.
            path = PathUtils.normalizePath(path);
            while (path != null)
            {
                for (String candidatePattern : patterns)
                {
                    if (PathUtils.prefixMatchesPathPattern(candidatePattern, path))
                    {
                        return path;
                    }
                }
                path = PathUtils.getParentPath(path);
            }
        }
        return null;
    }

    String getClosestOwningScope(CompositeType type, String path)
    {
        String scope = getClosestMatchingScope(type, path);
        if (scope != null)
        {
            return PathUtils.getParentPath(scope);
        }
        return scope;
    }

    List<String> getOwningPaths(CompositeType type, String prefix)
    {
        List<String> paths = compositeTypePathIndex.get(type);
        List<String> result = new LinkedList<String>();
        if (paths != null)
        {
            if( prefix == null)
            {
                // Include all
                result.addAll(paths);
            }
            else
            {
                // Restrict to those under prefix
                for (String owningPath : paths)
                {
                    if (PathUtils.prefixMatchesPathPattern(owningPath, prefix))
                    {
                        result.add(PathUtils.getPath(prefix, PathUtils.stripMatchingPrefix(owningPath, prefix)));
                    }
                }
            }
        }

        return result;
    }

    /**
     * Retrieve the type definition for the specified path.
     *
     * @param path the path to retrieve the type of
     * @return the type definition, or null if none exists.
     */
    public Type getType(String path)
    {
        String[] pathElements = PathUtils.getPathElements(path);
        String[] parentElements = PathUtils.getParentPathElements(pathElements);
        if (parentElements == null)
        {
            throw new IllegalArgumentException("Invalid path '" + path + "': no parent");
        }

        ConfigurationScopeInfo info = rootScopes.get(pathElements[0]);
        if (info == null)
        {
            throw new IllegalArgumentException("Invalid path '" + path + ": references non-existant root scope '" + pathElements[0] + "'");
        }

        if (parentElements.length == 0)
        {
            // Parent is the base, special case this as the base is currently
            // like a composite without a registered type :/.
            return info.getType();
        }
        else
        {
            String lastElement = pathElements[pathElements.length - 1];
            if(info.isPersistent())
            {
                return lookupPersistentType(path, parentElements, lastElement);
            }
            else
            {
                return lookupTransientType(path, pathElements, info);
            }
        }
    }

    private CompositeType lookupTransientType(String path, String[] pathElements, ConfigurationScopeInfo info)
    {
        CompositeType type = (CompositeType) info.getType();
        for(int i = 1; i < pathElements.length; i++)
        {
            TypeProperty property = type.getProperty(pathElements[i]);
            if(property == null)
            {
                throw new IllegalArgumentException("Invalid path '" + path + "': references non-existant property '" + pathElements[i] + "' of class '" + type.getClazz().getName() + "'");
            }

            Type propertyType = property.getType();
            if(!(propertyType instanceof CompositeType))
            {
                throw new IllegalArgumentException("Invalid path '" + path + "': references non-composite property '" + pathElements[i] + "' of class '" + type.getClazz().getName() + "'");
            }

            type = (CompositeType) propertyType;
        }

        return type;
    }

    private Type lookupPersistentType(String path, String[] parentElements, String lastElement)
    {
        Record parentRecord = recordManager.load(PathUtils.getPath(parentElements));
        if (parentRecord == null)
        {
            throw new IllegalArgumentException("Invalid path '" + path + "': parent does not exist");
        }

        String parentSymbolicName = parentRecord.getSymbolicName();
        Object value = parentRecord.get(lastElement);

        if (parentSymbolicName == null)
        {
            // Parent is a collection, last segment of path must refer to an
            // existing child composite record.
            if (value == null)
            {
                throw new IllegalArgumentException("Invalid path '" + path + "': references unknown child '" + lastElement + "' of collection");
            }
            // TODO: validate that collections must not contain collections
            return extractRecordType(value, path);
        }
        else
        {
            // Parent is a composite, see if the field exists.
            CompositeType parentType = typeRegistry.getType(parentSymbolicName);
            TypeProperty typeProperty = parentType.getProperty(lastElement);
            if (typeProperty == null)
            {
                throw new IllegalArgumentException("Invalid path '" + path + ": references non-existant field '" + lastElement + "' of type '" + parentSymbolicName + "'");
            }

            Type type = typeProperty.getType();
            if (value == null || type instanceof CollectionType)
            {
                return type;
            }
            else
            {
                // Return the type of the actual value.
                return extractRecordType(value, path);
            }
        }
    }

    private CompositeType extractRecordType(Object value, String path)
    {
        if (!(value instanceof Record))
        {
            throw new IllegalArgumentException("Invalid path '" + path + "': does not reference a complex type");
        }

        Record record = (Record) value;
        return typeRegistry.getType(record.getSymbolicName());
    }

    public <T extends Type> T getType(String path, Class<T> typeClass)
    {
        Type type = getType(path);
        if (type == null)
        {
            throw new IllegalArgumentException("Invalid path '" + path + "': does not exist");
        }

        if (!typeClass.isInstance(type))
        {
            throw new IllegalArgumentException("Invalid path '" + path + "': references incompatible type (expected '" + typeClass.getName() + "', found '" + type.getClass().getName() + "')");
        }

        return (T) type;
    }

    public List<String> getConfigurationPaths(CompositeType type)
    {
        return compositeTypePathIndex.get(type);
    }

    public boolean isPersistent(String path)
    {
        String[] parts = PathUtils.getPathElements(path);
        if(parts.length > 0)
        {
            ConfigurationScopeInfo rootScope = rootScopes.get(parts[0]);
            if(rootScope == null)
            {
                throw new IllegalArgumentException("Invalid path '" + path + "': references non-existant root scope '" + parts[0] + "'");
            }

            return rootScope.isPersistent();
        }
        else
        {
            throw new IllegalArgumentException("Invalid path: path is empty");
        }
    }

    public void setTypeRegistry(TypeRegistry typeRegistry)
    {
        this.typeRegistry = typeRegistry;
    }

    public void setRecordManager(RecordManager recordManager)
    {
        this.recordManager = recordManager;
    }
}
