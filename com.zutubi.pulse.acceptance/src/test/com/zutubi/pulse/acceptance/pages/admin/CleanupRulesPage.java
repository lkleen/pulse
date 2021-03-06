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

package com.zutubi.pulse.acceptance.pages.admin;

import com.zutubi.pulse.acceptance.SeleniumBrowser;
import com.zutubi.pulse.master.webwork.Urls;

/**
 * The list page representing the list of currently configured cleanup rules
 * for a specific project.
 */
public class CleanupRulesPage extends ListPage
{
    public CleanupRulesPage(SeleniumBrowser browser, Urls urls, String path)
    {
        super(browser, urls, path);
    }

    public String getSummary(int itemIndex)
    {
        return getCellContent(itemIndex, 1);
    }
}
