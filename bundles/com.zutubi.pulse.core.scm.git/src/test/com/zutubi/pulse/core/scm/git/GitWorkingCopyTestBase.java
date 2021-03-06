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

package com.zutubi.pulse.core.scm.git;

import com.google.common.io.Files;
import com.zutubi.pulse.core.scm.WorkingCopyContextImpl;
import com.zutubi.pulse.core.scm.api.ScmException;
import com.zutubi.pulse.core.scm.api.WorkingCopyContext;
import com.zutubi.pulse.core.test.api.PulseTestCase;
import com.zutubi.pulse.core.ui.TestUI;
import com.zutubi.pulse.core.util.PulseZipUtils;
import com.zutubi.util.SystemUtils;
import com.zutubi.util.config.PropertiesConfig;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static com.zutubi.pulse.core.scm.git.GitConstants.*;

public abstract class GitWorkingCopyTestBase extends PulseTestCase
{
    protected static final String REVISION_HEAD = "a69824696c9c9fa9383551b4a9a97653aed87483";
    protected static final String REVISION_EXPERIMENTAL = "9ac3ca040cf09a8979201ab37378c45ec7409180";
    protected static final String BRANCH_EXPERIMENTAL = "experimental";
    protected File tempDir;
    protected File baseDir;
    protected File otherDir;
    protected WorkingCopyContext context;
    protected GitWorkingCopy workingCopy;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        tempDir = createTempDirectory();

        File upstreamDir = new File(tempDir, "upstream");
        URL url = getClass().getResource("GitWorkingCopyTestBase.repo.zip");
        PulseZipUtils.extractZip(new File(url.toURI()), upstreamDir);

        baseDir = new File(tempDir, "base");
        runGit(tempDir, COMMAND_CLONE, upstreamDir.getName(), "base");

        otherDir = new File(tempDir, "other");
        runGit(tempDir, COMMAND_CLONE, upstreamDir.getName(), "other");

        context = new WorkingCopyContextImpl(baseDir, new PropertiesConfig(), new TestUI());
        workingCopy = new GitWorkingCopy();
    }

    @Override
    protected void tearDown() throws Exception
    {
        removeDirectory(tempDir);
        super.tearDown();
    }

    protected String runGit(File workingDir, String... command) throws IOException
    {
        List<String> commands = new LinkedList<String>();
        commands.add("git");
        commands.addAll(Arrays.asList(command));

        ProcessBuilder builder = new ProcessBuilder(commands);
        builder.directory(workingDir);
        return SystemUtils.runCommandWithInput(0, null, builder);
    }

    protected void switchToBranch(String branch) throws IOException
    {
        runGit(baseDir, COMMAND_CHECKOUT, FLAG_BRANCH, branch);
    }

    /**
     * Pushes a commit from the other clone and return the new latest revision.
     *
     * @return the new latest revision
     * @throws java.io.IOException on error editing a file
     * @throws com.zutubi.pulse.core.scm.git.GitException on git error
     */
    protected String otherPush() throws IOException, ScmException
    {

        editFile(otherDir, "file1");
        runGit(otherDir, COMMAND_PUSH);
        NativeGit nativeGit = new NativeGit();
        nativeGit.setWorkingDirectory(otherDir);
        return nativeGit.log(1).get(0).getId();
    }

    protected void editFile(File dir, String path) throws IOException
    {
        File f = new File(dir, path);
        Files.write("edited in " + getName(), f, Charset.defaultCharset());
        runGit(dir, COMMAND_COMMIT, FLAG_ALL, FLAG_MESSAGE, "made an edit");
    }

    protected void stageFile(File dir, String path) throws IOException
    {
        File f = new File(dir, path);
        Files.write("edited in " + getName(), f, Charset.defaultCharset());
        runGit(dir, COMMAND_ADD, path);
    }
}

// $ git log --name-status
//    commit a69824696c9c9fa9383551b4a9a97653aed87483
//    Author: Jason Sankey <jason@zutubi.com>
//    Date:   Mon Jun 22 15:38:57 2009 +0100
//
//        Another edit on master.
//
//    M	file1
//
//    commit 74d50a8ada25d15a2f829530d7580f0e4ae826bc
//    Author: Jason Sankey <jason@zutubi.com>
//    Date:   Mon Jun 22 15:33:29 2009 +0100
//
//        Changes on local branch.
//
//    M	dir1/nested
//    A	file3
//
//    commit 0cd9762c8e265acb548d6a667b49489a018a4bb9
//    Author: Jason Sankey <jason@zutubi.com>
//    Date:   Mon Jun 22 15:32:02 2009 +0100
//
//        Edit on master.
//
//    M	file1
//
//    commit 70017983e5eb11682398ca1cf5784cad5c8d5f5b
//    Author: Jason Sankey <jason@zutubi.com>
//    Date:   Mon Jun 22 15:30:50 2009 +0100
//
//        Initial import.
//
//    A	dir1/nested
//    A	file1
//    A	file2

// $ git log --name-status experimental
//    commit 9ac3ca040cf09a8979201ab37378c45ec7409180
//    Author: Jason Sankey <jason@zutubi.com>
//    Date:   Mon Jun 22 15:38:01 2009 +0100
//
//        Edits on experimental branch.
//
//    M	file2
//    D	file3
//
//    commit 74d50a8ada25d15a2f829530d7580f0e4ae826bc
//    Author: Jason Sankey <jason@zutubi.com>
//    Date:   Mon Jun 22 15:33:29 2009 +0100
//
//        Changes on local branch.
//
//    M	dir1/nested
//    A	file3
//
//    commit 0cd9762c8e265acb548d6a667b49489a018a4bb9
//    Author: Jason Sankey <jason@zutubi.com>
//    Date:   Mon Jun 22 15:32:02 2009 +0100
//
//        Edit on master.
//
//    M	file1
//
//    commit 70017983e5eb11682398ca1cf5784cad5c8d5f5b
//    Author: Jason Sankey <jason@zutubi.com>
//    Date:   Mon Jun 22 15:30:50 2009 +0100
//
//        Initial import.
//
//    A	dir1/nested
//    A	file1
//    A	file2
