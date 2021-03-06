/* Copyright 2017 Zutubi Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zutubi.pulse.core;

import com.zutubi.pulse.core.engine.api.BuildException;
import com.zutubi.tove.config.api.AbstractNamedConfiguration;
import com.zutubi.tove.config.api.Configuration;
import com.zutubi.util.bean.ObjectFactory;
import com.zutubi.util.logging.Logger;
import com.zutubi.util.reflection.ReflectionUtils;

import java.lang.reflect.Constructor;

/**
 * An abstract factory for creating instances using a constructor that takes a
 * configuration instance.
 */
public abstract class ConfiguredInstanceFactory<IType, CType extends Configuration>
{
    private static final Logger LOG = Logger.getLogger(ConfiguredInstanceFactory.class);

    private ObjectFactory objectFactory;

    public IType create(CType configuration)
    {
        // We need to pass the formal argument type to the object factory, so
        // figure it out from the actual type of the configuration.
        Class<?> formalParameterType = null;
        Class<? extends IType> type = getType(configuration);
        for (Constructor constructor: type.getConstructors())
        {
            if (ReflectionUtils.acceptsParameters(constructor, configuration.getClass()))
            {
                formalParameterType = constructor.getParameterTypes()[0];
            }
        }

        if (formalParameterType == null)
        {
            throw new BuildException("Unable to create instance '" + getName(configuration) + "': No constructor of type '" + type.getName() + "' accepts configuration of type '" + configuration.getClass().getName() + "'");
        }

        try
        {
            return objectFactory.buildBean(type, new Class[]{formalParameterType}, new Object[]{configuration});
        }
        catch (Exception e)
        {
            LOG.severe(e);
            throw new BuildException("Unable to create instance '" + getName(configuration) + "': " + e.getMessage(), e);
        }
    }

    private String getName(CType configuration)
    {
        if (configuration instanceof AbstractNamedConfiguration)
        {
            return ((AbstractNamedConfiguration) configuration).getName();
        }
        else
        {
            return "<unnamed>";
        }
    }

    protected abstract Class<? extends IType> getType(CType configuration);

    public void setObjectFactory(ObjectFactory objectFactory)
    {
        this.objectFactory = objectFactory;
    }
}