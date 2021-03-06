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

package com.zutubi.pulse.master.trigger;

import com.zutubi.pulse.master.tove.config.project.triggers.TriggerConditionConfiguration;

/**
 * Support base class for implementing {@link TriggerCondition}.
 */
public abstract class TriggerConditionSupport implements TriggerCondition
{
    private TriggerConditionConfiguration config;

    /**
     * Builds a condition, storing the configuration for later access.
     *
     * @param config configuration for this condition
     */
    protected TriggerConditionSupport(TriggerConditionConfiguration config)
    {
        this.config = config;
    }

    /**
     * Retrieves the configuration for this condition.
     *
     * @return this condition's configuration
     */
    public TriggerConditionConfiguration getConfig()
    {
        return config;
    }
}
