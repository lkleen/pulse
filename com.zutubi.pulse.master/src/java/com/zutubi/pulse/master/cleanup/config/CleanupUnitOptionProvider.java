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

package com.zutubi.pulse.master.cleanup.config;

import com.zutubi.tove.type.TypeProperty;
import com.zutubi.tove.ui.forms.EnumOptionProvider;
import com.zutubi.tove.ui.forms.FormContext;

/**
 * An enum option provider for the CleanupUnit enumeration that does not provide
 * the empty option as a possible selection. 
 */
public class CleanupUnitOptionProvider extends EnumOptionProvider
{
    public Option getEmptyOption(TypeProperty property, FormContext context)
    {
        return null;
    }
}
