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

package com.zutubi.i18n.context;

import java.io.InputStream;

/**
 * <class-comment/>
 */
public class ExtendedClassContext implements Context
{
    private final Class context;

    public ExtendedClassContext(Class context)
    {
        this.context = context;
    }

    public ExtendedClassContext(Object context)
    {
        this.context = getClass(context);
    }

    private Class getClass(Object context)
    {
        if (context == null)
        {
            throw new IllegalArgumentException();
        }
        if (context instanceof Class)
        {
            return (Class) context;
        }
        else
        {
            return context.getClass();
        }
    }

    public Class getContext()
    {
        return context;
    }

    public InputStream getResourceAsStream(String name)
    {
        return context.getResourceAsStream(name);
    }

    public boolean equals(Object other)
    {
        if (other == this)
        {
            return true;
        }
        if (!(other instanceof ExtendedClassContext))
        {
            return false;
        }
        ExtendedClassContext otherContext = (ExtendedClassContext) other;
        return context.equals(otherContext.context);
    }

    public int hashCode()
    {
        return context.hashCode();
    }

    public String toString()
    {
        return "<" + context.getName() + ">";
    }
}
