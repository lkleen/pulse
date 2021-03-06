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

package com.zutubi.pulse.master.model.persistence;

import com.zutubi.pulse.master.model.SequenceEntry;

/**
 * Data access interface for the sequence entries.
 */
public interface SequenceEntryDao extends EntityDao<SequenceEntry>
{
    /**
     * Retrieve a sequence entry by name.
     *
     * @param name  the name of the sequence
     * @return  the sequence entry instance, or null if non is found.
     */
    SequenceEntry findByName(String name);
}
