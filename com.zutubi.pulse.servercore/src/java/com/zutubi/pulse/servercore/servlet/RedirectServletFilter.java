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

package com.zutubi.pulse.servercore.servlet;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RedirectServletFilter implements Filter
{
    private static final String DESTINATION_PARAM_KEY = "destination";

    private String destination;

    public void destroy()
    {

    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException
    {
        if (destination != null)
        {
            HttpServletResponse response = (HttpServletResponse) servletResponse;
            response.sendRedirect(destination);
        }
    }

    public void init(FilterConfig config) throws ServletException
    {
        destination = config.getInitParameter(DESTINATION_PARAM_KEY);
        if (destination == null)
        {
            throw new ServletException("'destination' is a required init parameter");
        }
    }
}
