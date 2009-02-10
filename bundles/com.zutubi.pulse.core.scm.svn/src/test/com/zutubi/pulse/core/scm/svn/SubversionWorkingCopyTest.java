package com.zutubi.pulse.core.scm.svn;

import com.zutubi.pulse.core.personal.TestPersonalBuildUI;
import com.zutubi.pulse.core.scm.WorkingCopyContextImpl;
import com.zutubi.pulse.core.scm.api.*;
import com.zutubi.pulse.core.test.TestUtils;
import com.zutubi.pulse.core.test.api.PulseTestCase;
import com.zutubi.pulse.core.util.process.ProcessControl;
import com.zutubi.util.FileSystemUtils;
import com.zutubi.util.config.PropertiesConfig;
import com.zutubi.util.io.IOUtils;
import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 */
public class SubversionWorkingCopyTest extends PulseTestCase
{
    private File tempDir;
    private Process svnProcess;
    private File base;
    private WorkingCopyContext context;
    private SVNClientManager clientManager;
    private SVNUpdateClient updateClient;
    private File otherBase;
    private File branchBase;

    private SubversionWorkingCopy wc;
    private SVNWCClient client;

    static
    {
        // Initialise SVN library
        SVNRepositoryFactoryImpl.setup();
    }

    //    jsankey@shiny:/tmp/wc/trunk$ ls -R
    //    .:
    //    bin1  dir2  file1  file3  macfile1  script1  textfile1  unixfile1  winfile1
    //    dir1  dir3  file2  file4  macfile2  script2  textfile2  unixfile2  winfile2
    //
    //    ./dir1:
    //    file1  file2  file3
    //
    //    ./dir2:
    //    file1  file2
    //
    //    ./dir3:
    //    file1  nested
    //
    //    ./dir3/nested:
    //    file1  file2

    protected void setUp() throws Exception
    {
        tempDir = FileSystemUtils.createTempDir(getName(), "");
        tempDir = tempDir.getCanonicalFile();

        // Create empty repo
        File repoDir = new File(tempDir, "repo");
        repoDir.mkdir();
        svnProcess = Runtime.getRuntime().exec(new String[] { "svnadmin", "create", repoDir.getAbsolutePath() });
        svnProcess.waitFor();

        // Allow anonymous writes
        File conf = new File(repoDir, FileSystemUtils.composeFilename("conf", "svnserve.conf"));
        FileSystemUtils.createFile(conf, "[general]\nanon-access = write\nauth-access = write\n");

        // Restore from dump
        unzipInput("repo", tempDir);

        File dump = new File(tempDir, "SubversionWorkingCopyTest.dump");
        svnProcess = Runtime.getRuntime().exec(new String[] { "svnadmin", "load", "-q", repoDir.getAbsolutePath() });
        FileInputStream is = new FileInputStream(dump);
        IOUtils.joinStreams(is, svnProcess.getOutputStream());
        svnProcess.getOutputStream().close();
        is.close();
        svnProcess.waitFor();

        // Start svn server
        svnProcess = Runtime.getRuntime().exec(new String[] { "svnserve", "--foreground", "-dr", "."}, null, repoDir);
        TestUtils.waitForServer(3690);

        clientManager = SVNClientManager.newInstance();
        createWC();
    }

    private void createWC() throws ScmException, SVNException
    {
        base = new File(tempDir, "base");
        base.mkdir();
        updateClient = new SVNUpdateClient(new BasicAuthenticationManager("anonymous", ""), clientManager.getOptions());
        updateClient.doCheckout(SVNURL.parseURIDecoded("svn://localhost/test/trunk"), base, SVNRevision.UNDEFINED, SVNRevision.HEAD, SVNDepth.INFINITY, false);
        client = clientManager.getWCClient();
        wc = new SubversionWorkingCopy();
        wc.setUI(new TestPersonalBuildUI());
        context = new WorkingCopyContextImpl(base, new PropertiesConfig());
    }

    private void createOtherWC() throws SVNException
    {
        otherBase = new File(tempDir, "other");
        otherBase.mkdir();
        updateClient.doCheckout(SVNURL.parseURIDecoded("svn://localhost/test/trunk"), otherBase, SVNRevision.UNDEFINED, SVNRevision.HEAD, SVNDepth.INFINITY, false);
    }

    private void createBranchWC() throws SVNException
    {
        branchBase = new File(tempDir, "branch");
        branchBase.mkdir();
        updateClient.doCheckout(SVNURL.parseURIDecoded("svn://localhost/test/branches/2"), branchBase, SVNRevision.UNDEFINED, SVNRevision.HEAD, SVNDepth.INFINITY, false);
    }

    protected void tearDown() throws Exception
    {
        ProcessControl.destroyProcess(svnProcess);
        svnProcess.waitFor();
        svnProcess = null;
        Thread.sleep(100);

        FileSystemUtils.rmdir(tempDir);

        updateClient = null;
        client = null;
        wc = null;
    }

    public void testMatchesLocationMatches() throws ScmException
    {
        assertTrue(wc.matchesLocation(context, "svn://localhost/test/trunk"));
    }

    public void testMatchesLocationDoesntMatch() throws ScmException
    {
        assertFalse(wc.matchesLocation(context, "svn://localhost/test/branches/1.0.x"));
    }

    public void testMatchesLocationEmbeddedUser() throws ScmException
    {
        assertTrue(wc.matchesLocation(context, "svn://goober@localhost/test/trunk"));
    }

    public void testGetLocalStatusNoChanges() throws Exception
    {
        WorkingCopyStatus wcs = wc.getLocalStatus(context);
        assertEquals(0, wcs.getFileStatuses().size());
    }

    public void testGetLocalStatusEdited() throws Exception
    {
        edit("file1");
        WorkingCopyStatus wcs = assertSimpleStatus("file1", FileStatus.State.MODIFIED);
        assertNoProperties(wcs, "file1");
    }

    public void testGetLocalStatusEditedText() throws Exception
    {
        File test = new File(base, "textfile1");
        FileSystemUtils.createFile(test, "hello");
        WorkingCopyStatus wcs = assertSimpleStatus("textfile1", FileStatus.State.MODIFIED);
        assertEOL(wcs, "textfile1", EOLStyle.NATIVE);
    }

    public void testGetLocalStatusEditedNewlyText() throws Exception
    {
        File test = edit("file1");
        client.doSetProperty(test, SubversionConstants.SVN_PROPERTY_EOL_STYLE, SVNPropertyValue.create("native"), true, SVNDepth.EMPTY, null, null);

        WorkingCopyStatus wcs = assertSimpleStatus("file1", FileStatus.State.MODIFIED);
        assertEOL(wcs, "file1", EOLStyle.NATIVE);
    }

    public void testGetLocalStatusEditedLF() throws Exception
    {
        File test = new File(base, "unixfile1");
        FileSystemUtils.createFile(test, "hello");
        WorkingCopyStatus wcs = assertSimpleStatus("unixfile1", FileStatus.State.MODIFIED);
        assertEOL(wcs, "unixfile1", EOLStyle.LINEFEED);
    }

    public void testGetLocalStatusEditedAddedRandomProperty() throws Exception
    {
        File test = edit("file1");
        client.doSetProperty(test, "random", SVNPropertyValue.create("value"), true, SVNDepth.EMPTY, null, null);
        WorkingCopyStatus wcs = assertSimpleStatus("file1", FileStatus.State.MODIFIED);
        assertNoProperties(wcs, "file1");
    }

    public void testGetLocalStatusEditedAddedExecutableProperty() throws Exception
    {
        File test = edit("file1");
        client.doSetProperty(test, SubversionConstants.SVN_PROPERTY_EXECUTABLE, SVNPropertyValue.create("yay"), true, SVNDepth.EMPTY, null, null);
        WorkingCopyStatus wcs = assertSimpleStatus("file1", FileStatus.State.MODIFIED);
        assertExecutable(wcs, "file1", true);
    }

    public void testGetLocalStatusEditedRemovedExecutableProperty() throws Exception
    {
        File test = new File(base, "bin1");
        FileSystemUtils.createFile(test, "hello");
        client.doSetProperty(test, SubversionConstants.SVN_PROPERTY_EXECUTABLE, null, true, SVNDepth.EMPTY, null, null);
        WorkingCopyStatus wcs = assertSimpleStatus("bin1", FileStatus.State.MODIFIED);
        assertExecutable(wcs, "bin1", false);
    }

    public void testGetLocalStatusAdded() throws Exception
    {
        File test = new File(base, "newfile");
        FileSystemUtils.createFile(test, "hello");

        client.doAdd(test, true, false, false, SVNDepth.EMPTY, false, false);
        WorkingCopyStatus wcs = assertSimpleStatus("newfile", FileStatus.State.ADDED);
        assertNoProperties(wcs, "newfile");
    }

    public void testGetLocalStatusAddedText() throws Exception
    {
        File test = new File(base, "newfile");
        FileSystemUtils.createFile(test, "hello");

        client.doAdd(test, true, false, false, SVNDepth.EMPTY, false, false);
        client.doSetProperty(test, SubversionConstants.SVN_PROPERTY_EOL_STYLE, SVNPropertyValue.create("native"), true, SVNDepth.EMPTY, null, null);
        WorkingCopyStatus wcs = assertSimpleStatus("newfile", FileStatus.State.ADDED);
        assertEOL(wcs, "newfile", EOLStyle.NATIVE);
    }

    public void testGetLocalStatusAddedDirectory() throws Exception
    {
        File test = new File(base, "newdir");
        assertTrue(test.mkdir());

        client.doAdd(test, true, false, false, SVNDepth.EMPTY, false, false);
        WorkingCopyStatus status = wc.getLocalStatus(context);
        assertEquals(1, status.getFileStatuses().size());
        assertAdded(status, "newdir", true);
    }

    public void testGetLocalStatusAddedDirectoryChildFile() throws Exception
    {
        File dir = new File(base, "newdir");
        assertTrue(dir.mkdir());

        File child = new File(dir, "newfile");
        FileSystemUtils.createFile(child, "test");

        client.doAdd(dir, true, false, false, SVNDepth.INFINITY, false, false);
        WorkingCopyStatus status = wc.getLocalStatus(context);
        assertEquals(2, status.getFileStatuses().size());
        assertAdded(status, "newdir", true);
        assertAdded(status, "newdir/newfile", false);
    }

    public void testGetLocalStatusAddedDirectoryChildFileNotAdded() throws Exception
    {
        File dir = new File(base, "newdir");
        assertTrue(dir.mkdir());

        File child = new File(dir, "newfile");
        FileSystemUtils.createFile(child, "test");

        client.doAdd(dir, true, false, false, SVNDepth.EMPTY, false, false);
        WorkingCopyStatus status = wc.getLocalStatus(context);
        assertEquals(1, status.getFileStatuses().size());
        assertAdded(status, "newdir", true);
    }

    public void testGetLocalStatusMoved() throws Exception
    {
        move("file1", "movedfile1");

        WorkingCopyStatus status = wc.getLocalStatus(context);
        assertEquals(2, status.getFileStatuses().size());
        assertDeleted(status, "file1", false);
        assertAdded(status, "movedfile1", false);
    }

    public void testGetLocalStatusMovedDir() throws Exception
    {
        move("dir1", "moveddir1");

        WorkingCopyStatus status = wc.getLocalStatus(context);
        assertEquals(8, status.getFileStatuses().size());
        assertDeleted(status, "dir1", true);
        assertDeleted(status, "dir1/file1", false);
        assertDeleted(status, "dir1/file2", false);
        assertDeleted(status, "dir1/file3", false);
        assertAdded(status, "moveddir1", true);
        assertAdded(status, "moveddir1/file1", false);
        assertAdded(status, "moveddir1/file2", false);
        assertAdded(status, "moveddir1/file3", false);
    }

    public void testGetLocalStatusMovedDirNestedChild() throws Exception
    {
        move("dir3", "moveddir3");

        WorkingCopyStatus status = wc.getLocalStatus(context);
        assertEquals(10, status.getFileStatuses().size());
        assertDeleted(status, "dir3", true);
        assertDeleted(status, "dir3/file1", false);
        assertDeleted(status, "dir3/nested", true);
        assertDeleted(status, "dir3/nested/file1", false);
        assertDeleted(status, "dir3/nested/file2", false);
        assertAdded(status, "moveddir3", true);
        assertAdded(status, "moveddir3/file1", false);
        assertAdded(status, "moveddir3/nested", true);
        assertAdded(status, "moveddir3/nested/file1", false);
        assertAdded(status, "moveddir3/nested/file2", false);
    }

    public void testGetLocalStatusMovedDirModifiedChild() throws Exception
    {
        move("dir2", "moveddir2");
        edit("moveddir2/file1");

        WorkingCopyStatus status = wc.getLocalStatus(context);
        assertEquals(6, status.getFileStatuses().size());
        assertDeleted(status, "dir2", true);
        assertDeleted(status, "dir2/file1", false);
        assertDeleted(status, "dir2/file2", false);
        assertAdded(status, "moveddir2", true);
        assertModified(status, "moveddir2/file1", false);
        assertAdded(status, "moveddir2/file2", false);
    }

    public void testGetLocalStatusMovedDirDeletedChild() throws Exception
    {
        move("dir2", "moveddir2");
        delete("moveddir2/file1");

        WorkingCopyStatus status = wc.getLocalStatus(context);
        assertEquals(6, status.getFileStatuses().size());
        assertDeleted(status, "dir2", true);
        assertDeleted(status, "dir2/file1", false);
        assertDeleted(status, "dir2/file2", false);
        assertAdded(status, "moveddir2", true);
        assertDeleted(status, "moveddir2/file1", false);
        assertAdded(status, "moveddir2/file2", false);
    }

    public void getLocalStatusMergeEdited() throws Exception
    {
        long rev = branchEdit("file1");
        doMerge(rev);
        assertSimpleStatus("file1", FileStatus.State.MODIFIED);
    }

    public void getLocalStatusMergeAdded() throws Exception
    {
        long rev = branchAdd("newfile");
        doMerge(rev);
        assertSimpleStatus("newfile", FileStatus.State.ADDED);
    }

    public void getLocalStatusMergeDeleted() throws Exception
    {
        long rev = branchDelete("file1");
        doMerge(rev);
        assertSimpleStatus("file1", FileStatus.State.DELETED);
    }

    public void getLocalStatusMergeMoved() throws Exception
    {
        long rev = branchMove("file1", "movedfile1");
        doMerge(rev);
        WorkingCopyStatus status = wc.getLocalStatus(context);
        assertEquals(2, status.getFileStatuses().size());
        assertDeleted(status, "file1", false);
        assertAdded(status, "movedfile1", false);
    }

    public void getLocalStatusMergeMovedDir() throws Exception
    {
        long rev = branchMove("dir2", "moveddir2");
        doMerge(rev);
        WorkingCopyStatus status = wc.getLocalStatus(context);
        assertEquals(6, status.getFileStatuses().size());
        assertDeleted(status, "dir2", true);
        assertDeleted(status, "dir2/file1", false);
        assertDeleted(status, "dir2/file2", false);
        assertAdded(status, "moveddir2", true);
        assertAdded(status, "moveddir2/file1", false);
        assertAdded(status, "moveddir2/file2", false);
    }

    public void getLocalStatusMergeConflict() throws Exception
    {
        edit("file1");
        long rev = branchEdit("file1");
        doMerge(rev);
        assertSimpleStatus("file1", FileStatus.State.UNRESOLVED);
    }

    public void getLocalStatusMergeEditDeleted() throws Exception
    {
        edit("file1");
        long rev = branchDelete("file1");
        doMerge(rev);
        // Forced merge deletes locally edited file
        assertSimpleStatus("file1", FileStatus.State.DELETED);
    }

    public void testGetLocalStatusDeleted() throws Exception
    {
        delete("file1");
        WorkingCopyStatus wcs = assertSimpleStatus("file1", FileStatus.State.DELETED);
        assertNoProperties(wcs, "file1");
    }

    public void testGetLocalStatusUnchangedOOD() throws Exception
    {
        otherEdit("file1");
        WorkingCopyStatus wcs = wc.getLocalStatus(context);
        assertEquals(0, wcs.getFileStatuses().size());
    }

    public void testGetStatusEditedOOD() throws Exception
    {
        otherEdit("file1");
        edit("file1");
        WorkingCopyStatus wcs = assertSimpleStatus("file1", FileStatus.State.MODIFIED);
        assertNoProperties(wcs, "file1");
    }

    public void testGetLocalStatusEditedOOD() throws Exception
    {
        otherEdit("file1");
        edit("file1");
        assertSimpleStatus("file1", FileStatus.State.MODIFIED);
    }

    public void testGetLocalStatusEditDeleted() throws Exception
    {
        otherDelete("file1");
        edit("file1");
        assertSimpleStatus("file1", FileStatus.State.MODIFIED);
    }

    public void testGetStatusDeleteEdited() throws Exception
    {
        otherEdit("file1");
        delete("file1");
        WorkingCopyStatus wcs = assertSimpleStatus("file1", FileStatus.State.DELETED);
        assertNoProperties(wcs, "file1");
    }

    public void testGetLocalStatusDeleteEdited() throws Exception
    {
        otherEdit("file1");
        delete("file1");
        assertSimpleStatus("file1", FileStatus.State.DELETED);
    }

    public void testGetLocalStatusRestrictedToUnchanged() throws Exception
    {
        edit("file1");
        WorkingCopyStatus wcs = wc.getLocalStatus(context, "dir1", "script1");
        assertEquals(0, wcs.getFileStatuses().size());
    }

    public void testGetLocalStatusRestrictedToFiles() throws Exception
    {
        edit("bin1");
        edit("file1");
        edit("script1");
        WorkingCopyStatus wcs = wc.getLocalStatus(context, "bin1", "script1");
        assertEquals(2, wcs.getFileStatuses().size());
        assertEquals("bin1", wcs.getFileStatuses().get(0).getPath());
        assertEquals("script1", wcs.getFileStatuses().get(1).getPath());
    }

    public void testGetLocalStatusRecurses() throws Exception
    {
        edit("dir1/file1");
        edit("dir1/file2");
        WorkingCopyStatus wcs = wc.getLocalStatus(context, "dir1");
        assertEquals(2, wcs.getFileStatuses().size());
        assertEquals("dir1/file1", wcs.getFileStatuses().get(0).getPath());
        assertEquals("dir1/file2", wcs.getFileStatuses().get(1).getPath());
    }

    public void testUpdateAlreadyUpToDate() throws Exception
    {
        wc.update(context, Revision.HEAD);
        testGetLocalStatusNoChanges();
    }

    public void testUpdateBasic() throws Exception
    {
        otherEdit("file1");

        wc.update(context, Revision.HEAD);
        WorkingCopyStatus wcs = wc.getLocalStatus(context);
        assertEquals(0, wcs.getFileStatuses().size());
    }

    public void testUpdateConflict() throws Exception
    {
        otherEdit("file1");

        File test = new File(base, "file1");
        FileSystemUtils.createFile(test, "goodbye");
        wc.update(context, Revision.HEAD);
        assertSimpleStatus("file1", FileStatus.State.UNRESOLVED);
    }

    private File edit(String path) throws IOException, SVNException
    {
        doEdit(path, base, false);
        return new File(base, path);
    }

    private void otherEdit(String path) throws SVNException, IOException
    {
        createOtherWC();
        doEdit(path, otherBase, true);
    }

    private long branchEdit(String path) throws SVNException, IOException
    {
        createBranchWC();
        return doEdit(path, branchBase, true);
    }

    private long doEdit(String path, File baseDir, boolean commit) throws IOException, SVNException
    {
        File test = new File(baseDir, path);
        FileSystemUtils.createFile(test, test.getAbsolutePath());
        if(commit)
        {
            SVNCommitInfo info = clientManager.getCommitClient().doCommit(new File[] { test }, true, "edit file", null, null, false, false, SVNDepth.EMPTY);
            return info.getNewRevision();
        }
        return -1;
    }

    private long branchAdd(String path) throws SVNException, IOException
    {
        createBranchWC();
        return doAdd(path, branchBase, true);
    }

    private long doAdd(String path, File baseDir, boolean commit) throws IOException, SVNException
    {
        File test = new File(baseDir, path);
        FileSystemUtils.createFile(test, test.getAbsolutePath());
        clientManager.getWCClient().doAdd(test, true, false, false, SVNDepth.INFINITY, false, false);

        if(commit)
        {
            SVNCommitInfo info = clientManager.getCommitClient().doCommit(new File[] { test }, true, "add file", null, null, false, false, SVNDepth.EMPTY);
            return info.getNewRevision();
        }
        return -1;
    }

    private void delete(String path) throws SVNException
    {
        doDelete(path, base, false);
    }

    private void otherDelete(String path) throws SVNException
    {
        createOtherWC();
        doDelete(path, otherBase, true);
    }

    private long branchDelete(String path) throws SVNException
    {
        createBranchWC();
        return doDelete(path, branchBase, true);
    }

    private long doDelete(String path, File baseDir, boolean commit) throws SVNException
    {
        File test = new File(baseDir, path);
        client.doDelete(test, false, false);
        if(commit)
        {
            SVNCommitInfo info = clientManager.getCommitClient().doCommit(new File[] { test }, true, "delete file", null, null, false, false, SVNDepth.EMPTY);
            return info.getNewRevision();
        }
        return -1;
    }

    private long move(String srcPath, String destPath) throws SVNException, IOException
    {
        return doMove(srcPath, destPath, base, false);
    }

    private long branchMove(String srcPath, String destPath) throws SVNException, IOException
    {
        createBranchWC();
        return doMove(srcPath, destPath, branchBase, true);
    }

    private long doMove(String srcPath, String destPath, File baseDir, boolean commit) throws IOException, SVNException
    {
        clientManager.getMoveClient().doMove(new File(baseDir, srcPath), new File(baseDir, destPath));

        if(commit)
        {
            SVNCommitInfo info = clientManager.getCommitClient().doCommit(new File[] { baseDir }, true, "move", null, null, false, false, SVNDepth.INFINITY);
            return info.getNewRevision();
        }
        return -1;
    }

    private void doMerge(long change) throws SVNException
    {
        SVNURL branchUrl = SVNURL.parseURIDecoded("svn://localhost/test/branches/2");
        clientManager.getDiffClient().doMerge(branchUrl, SVNRevision.create(change - 1), branchUrl, SVNRevision.create(change), base, SVNDepth.INFINITY, false, true, false, false);
    }

    private WorkingCopyStatus assertSimpleStatus(String path, FileStatus.State state) throws ScmException
    {
        WorkingCopyStatus wcs = wc.getLocalStatus(context);
        assertEquals(1, wcs.getFileStatuses().size());
        FileStatus fs = wcs.getFileStatus(path);
        assertEquals(state, fs.getState());
        assertFalse(fs.isDirectory());
        return wcs;
    }

    private void assertModified(WorkingCopyStatus status, String path, boolean dir)
    {
        assertFileStatus(status, path, dir, FileStatus.State.MODIFIED);
    }

    private void assertAdded(WorkingCopyStatus status, String path, boolean dir)
    {
        assertFileStatus(status, path, dir, FileStatus.State.ADDED);
    }

    private void assertDeleted(WorkingCopyStatus status, String path, boolean dir)
    {
        assertFileStatus(status, path, dir, FileStatus.State.DELETED);
    }

    private void assertFileStatus(WorkingCopyStatus status, String path, boolean dir, FileStatus.State fileState)
    {
        FileStatus fs = status.getFileStatus(path);
        assertNotNull(fs);
        assertEquals(dir, fs.isDirectory());
        assertEquals(fileState, fs.getState());
    }

    private void assertNoProperties(WorkingCopyStatus wcs, String path)
    {
        assertEquals(0, wcs.getFileStatus(path).getProperties().size());
    }

    private void assertEOL(WorkingCopyStatus wcs, String path, EOLStyle eol)
    {
        FileStatus fs = wcs.getFileStatus(path);
        assertEquals(eol.toString(), fs.getProperty(FileStatus.PROPERTY_EOL_STYLE));
    }

    private void assertExecutable(WorkingCopyStatus wcs, String path, boolean executable)
    {
        FileStatus fs = wcs.getFileStatus(path);
        assertEquals(Boolean.toString(executable), fs.getProperty(FileStatus.PROPERTY_EXECUTABLE));
    }
}