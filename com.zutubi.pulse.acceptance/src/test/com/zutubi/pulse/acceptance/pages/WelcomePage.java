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

package com.zutubi.pulse.acceptance.pages;

import com.zutubi.pulse.acceptance.IDs;
import com.zutubi.pulse.acceptance.SeleniumBrowser;
import com.zutubi.pulse.master.webwork.Urls;

/**
 */
public class WelcomePage extends SeleniumPage
{
    public WelcomePage(SeleniumBrowser browser, Urls urls)
    {
        super(browser, urls, "welcome.heading", "welcome");
    }

    public String getUrl()
    {
        return urls.base();
    }

    public boolean isLogoutLinkPresent()
    {
        return isElementIdPresent(IDs.ID_LOGOUT);
    }

    public boolean isLoginLinkPresent()
    {
        return isElementIdPresent(IDs.ID_LOGIN);
    }

}
