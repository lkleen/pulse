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

package com.zutubi.pulse.master.upgrade.tasks;

import com.zutubi.tove.type.record.PathUtils;

import java.util.Arrays;
import java.util.List;

/**
 * Adds new properties to dashboard preferences as it is upgraded to come
 * inline with the new browse view.
 */
public class DashboardBuildPreferencesUpgradeTask extends AbstractRecordPropertiesUpgradeTask
{
    // Scope and path to the records we are interested in
    private static final String SCOPE_USERS = "users";
    private static final String PROPERTY_PREFERENCES = "preferences";
    private static final String PROPERTY_DASHBOARD = "dashboard";

    // New properties
    private static final String PROPERTY_GROUPS_SHOWN = "groupsShown";
    private static final String PROPERTY_HIERARCHY_SHOWN = "hierarchyShown";
    private static final String PROPERTY_HIDDEN_HIERARCHY_LEVELS = "hiddenHierarchyLevels";
    private static final String PROPERTY_COLUMNS = "columns";
    private static final String[] DEFAULT_COLUMNS = new String[]{"when", "elapsed", "reason", "tests"};

    // Renamed property
    private static final String PROPERTY_BUILD_COUNT = "buildCount";
    private static final String PROPERTY_BUILDS_PER_PROJECT = "buildsPerProject";

    public boolean haltOnFailure()
    {
        return true;
    }

    protected RecordLocator getRecordLocator()
    {
        return RecordLocators.newPathPattern(PathUtils.getPath(SCOPE_USERS, PathUtils.WILDCARD_ANY_ELEMENT, PROPERTY_PREFERENCES, PROPERTY_DASHBOARD));
    }

    protected List<? extends RecordUpgrader> getRecordUpgraders()
    {
        return Arrays.asList(
                RecordUpgraders.newAddProperty(PROPERTY_GROUPS_SHOWN, Boolean.toString(true)),
                RecordUpgraders.newAddProperty(PROPERTY_HIERARCHY_SHOWN, Boolean.toString(true)),
                RecordUpgraders.newAddProperty(PROPERTY_HIDDEN_HIERARCHY_LEVELS, Integer.toString(1)),
                RecordUpgraders.newAddProperty(PROPERTY_COLUMNS, DEFAULT_COLUMNS),
                RecordUpgraders.newRenameProperty(PROPERTY_BUILD_COUNT, PROPERTY_BUILDS_PER_PROJECT)
        );
    }
}