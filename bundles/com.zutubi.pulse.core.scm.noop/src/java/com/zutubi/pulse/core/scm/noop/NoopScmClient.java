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

package com.zutubi.pulse.core.scm.noop;

import com.zutubi.pulse.core.engine.api.ExecutionContext;
import com.zutubi.pulse.core.engine.api.ResourceProperty;
import com.zutubi.pulse.core.scm.api.*;
import com.zutubi.pulse.core.scm.config.api.ScmConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * A stub ScmClient that does nothing.
 */
public class NoopScmClient implements ScmClient
{
    public String getImplicitResource()
    {
        return null;
    }

    public void init(ScmContext context, ScmFeedbackHandler handler) throws ScmException
    {
    }

    public void destroy(ScmContext context, ScmFeedbackHandler handler) throws ScmException
    {
    }

    public void close()
    {
    }

    public Set<ScmCapability> getCapabilities(ScmContext context)
    {
        return Collections.emptySet();
    }

    public String getUid(ScmContext context) throws ScmException
    {
        return "no-op.scm";
    }

    public String getLocation(ScmContext context)
    {
        return "[none]";
    }

    public List<ResourceProperty> getProperties(ExecutionContext context) throws ScmException
    {
        return Collections.emptyList();
    }

    public void testConnection() throws ScmException
    {
    }

    public Revision checkout(ExecutionContext context, Revision revision, ScmFeedbackHandler handler) throws ScmException
    {
        return null;
    }

    public InputStream retrieve(ScmContext context, String path, Revision revision) throws ScmException
    {
        return new InputStream()
        {
            @Override
            public int read() throws IOException
            {
                return -1;
            }
        };
    }

    public List<Changelist> getChanges(ScmContext context, Revision from, Revision to) throws ScmException
    {
        throw new ScmException("Operation not supported");
    }

    public List<Revision> getRevisions(ScmContext context, Revision from, Revision to) throws ScmException
    {
        throw new ScmException("Operation not supported");
    }

    public Revision getLatestRevision(ScmContext context) throws ScmException
    {
        throw new ScmException("Operation not supported");
    }

    public List<ScmFile> browse(ScmContext context, String path, Revision revision) throws ScmException
    {
        throw new ScmException("Operation not supported");
    }

    public Revision update(ExecutionContext context, Revision rev, ScmFeedbackHandler handler) throws ScmException
    {
        return checkout(context, rev, handler);
    }

    public void tag(ScmContext scmContent, Revision revision, String name, boolean moveExisting) throws ScmException
    {
        throw new ScmException("Operation not supported");
    }

    public void storeConnectionDetails(ExecutionContext context, File outputDir) throws ScmException, IOException
    {
    }

    public EOLStyle getEOLPolicy(ExecutionContext context)
    {
        return EOLStyle.BINARY;
    }

    public Revision parseRevision(ScmContext context, String revision) throws ScmException
    {
        throw new ScmException("Operation not supported");
    }

    public Revision getPreviousRevision(ScmContext context, Revision revision, boolean isFile) throws ScmException
    {
        throw new ScmException("Operation not supported");
    }

    public String getEmailAddress(ScmContext context, String user) throws ScmException
    {
        throw new ScmException("Operation not supported");
    }

    public boolean configChangeRequiresClean(ScmConfiguration oldConfig, ScmConfiguration newConfig)
    {
        return false;
    }
}
