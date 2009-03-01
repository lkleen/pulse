package com.zutubi.pulse.servercore.hessian;

import com.caucho.hessian.io.*;
import com.zutubi.util.CollectionUtils;
import com.zutubi.util.Predicate;
import com.zutubi.util.ReflectionUtils;
import com.zutubi.util.Pair;
import com.zutubi.pulse.servercore.dependency.ivy.ModuleDescriptorSerialiser;
import com.zutubi.pulse.servercore.dependency.ivy.ModuleDescriptorDeserialiser;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import org.apache.ivy.core.module.descriptor.ModuleDescriptor;

/**
 * A serialiser factory that handles Java 5 enums.
 *
 * Adapted from an implementation posted to hessian-interest by Jason Stiefel.
 */
public class CustomSerialiserFactory extends AbstractSerializerFactory
{
    private final Map<Class, Serializer> serialisers = new HashMap<Class, Serializer>();
    private final Map<Class, Deserializer> deserialisers = new HashMap<Class, Deserializer>();

    public CustomSerialiserFactory()
    {
        serialisers.put(Enum.class, new EnumSerialiser());

        deserialisers.put(Enum.class, new EnumDeserialiser());
        deserialisers.put(LogRecord.class, new CustomDeserialiser(LogRecord.class));
        deserialisers.put(Level.class, new CustomDeserialiser(Level.class));
    }

    public Serializer getSerializer(Class cl) throws HessianProtocolException
    {
        return lookup(cl, serialisers);
    }

    public Deserializer getDeserializer(Class cl)
    {
        return lookup(cl, deserialisers);
    }

    private <T> T lookup(Class cl, final Map<Class, T> map)
    {
        // note, since the getAssignableTo returns a set of an unspecified order,
        // if a class has multiple possible de/serialisers registered, the first
        // one encountered will be used.  This is not necessarily the best output
        // but is sufficient at the time of writing.
        Class closestAssignable = CollectionUtils.find(getAssignableTo(cl), new Predicate<Class>()
        {
            public boolean satisfied(Class aClass)
            {
                return map.containsKey(aClass);
            }
        });

        if (closestAssignable != null)
        {
            return map.get(closestAssignable);
        }
        return null;
    }

    public void register(Class cl, Serializer serialiser, Deserializer deserialiser)
    {
        serialisers.put(cl, serialiser);
        deserialisers.put(cl, deserialiser);
    }

    public Pair<Serializer, Deserializer> deregister(Class cl)
    {
        return new Pair<Serializer, Deserializer>(serialisers.remove(cl), deserialisers.remove(cl));
    }

    /**
     * Returns a collection of classes to which the specified class is assignable.
     *
     * @param cls   the class in question
     * @return      the set of types this class is assignable to
     */
    private Set<Class> getAssignableTo(Class cls)
    {
        return ReflectionUtils.getSupertypes(cls, Object.class, false);
    }
}