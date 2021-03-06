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

package com.zutubi.tove.config.api;

/**
 * Represents a named variant of a configuration action.  Variants are used when a common action
 * can be configured in multiple ways by the user for different presentation/behaviour in UIs.
 * Importantly, variants can be configured differently depending on the target instance, not
 * statically based on the instance's type.
 * <p/>
 * Variants may be ignored in non-interactive interfaces (such as APIs) if desired.
 */
public class ActionVariant
{
    private String name;
    private boolean hasArgument;

    /**
     * Creates a variant.
     *
     * @param name name of the variant to display to the user
     * @param hasArgument true iff executing this variant requires an argument (beyond the name)
     */
    public ActionVariant(String name, boolean hasArgument)
    {
        this.name = name;
        this.hasArgument = hasArgument;
    }

    /**
     * @return the variant name, which is used both to label it in the UI and as an input argument
     *         to the action
     */
    public String getName()
    {
        return name;
    }

    /**
     * @return true if this variant requires input beyond the variant name to be executed
     */
    public boolean hasArgument()
    {
        return hasArgument;
    }
}
