package com.zutubi.pulse.acceptance;

import com.zutubi.pulse.acceptance.forms.admin.AddProjectWizard;
import com.zutubi.pulse.acceptance.forms.admin.AntTypeForm;
import com.zutubi.pulse.acceptance.windows.BrowseScmWindow;
import com.zutubi.pulse.master.model.ProjectManager;

import java.io.File;
import java.util.Hashtable;

/**
 * A high level acceptance test that checks the ability to browse and select
 * files and directories from the scm repository.
 */
public class BrowseScmAcceptanceTest extends SeleniumTestBase
{
    protected void setUp() throws Exception
    {
        super.setUp();

        loginAsAdmin();
    }

    protected void tearDown() throws Exception
    {
        // if something goes wrong whilst focused on the browse window, then
        // logout fails with an error.  Avoid this by resetting the focused window.
        resetToPulseWindow();

        logout();
        
        super.tearDown();
    }

    private void resetToPulseWindow()
    {
        String[] titles = selenium.getAllWindowTitles();
        for (String title : titles)
        {
            if (title.startsWith(":: pulse ::"))
            {
                selenium.selectWindow(title);
                return;
            }
        }
    }

    //---( Test the rendering of the 'browse' link on the project wizard type form )---

    public void testBrowseLinkAvailableInProjectWizardForSubversion()
    {
        runAddProjectWizard(new DefaultProjectWizardDriver(ProjectManager.GLOBAL_PROJECT_NAME, random, false)
        {
            public void typeState(AddProjectWizard.TypeState form)
            {
                AddProjectWizard.AntState ant = (AddProjectWizard.AntState) form;
                assertTrue(ant.isBrowseFileAvailable());
                assertTrue(ant.isBrowseWorkAvailable());
                ant.cancel();
            }
        });
    }

    public void testBrowseLinkNotAvailableInProjectWizardForGit()
    {
        runAddProjectWizard(new DefaultProjectWizardDriver(ProjectManager.GLOBAL_PROJECT_NAME, random, false)
        {
            public String selectScm()
            {
                return "zutubi.gitConfig";
            }

            public void scmState(AddProjectWizard.ScmState form)
            {
                form.nextFormElements("file://doesnt/matter/", "master", "CLEAN_CHECKOUT");
            }

            public void typeState(AddProjectWizard.TypeState form)
            {
                AddProjectWizard.AntState ant = (AddProjectWizard.AntState) form;
                assertFalse(ant.isBrowseFileAvailable());
                assertFalse(ant.isBrowseWorkAvailable());
                ant.cancel();
            }
        });
    }

    //---( test rendering browse link is available in the project type configuration pages. )---

    public void testBrowseLinkAvailableForSubversionAntProjectConfiguration() throws Exception
    {
        AntTypeForm antForm = insertTestSvnProjectAndNavigateToTypeConfig();
        assertTrue(antForm.isBrowseBuildFileLinkPresent());
        assertTrue(antForm.isBrowseWorkingDirectoryLinkPresent());
    }

    public void testBrowseLinkAvailableForGitAntProjectConfiguration() throws Exception
    {
        AntTypeForm antForm = insertTestGitProjectAndNavigateToTypeConfig();
        assertTrue(antForm.isBrowseBuildFileLinkPresent());
        assertTrue(antForm.isBrowseWorkingDirectoryLinkPresent());
    }

    //---( test the browse window . )---

    public void testBrowseSelectionOfScmFile() throws Exception
    {
        AntTypeForm antForm = insertTestSvnProjectAndNavigateToTypeConfig();
        assertEquals("build.xml", antForm.getBuildFileFieldValue());
        assertTrue(antForm.isBrowseBuildFileLinkPresent());

        BrowseScmWindow browse = antForm.clickBrowseBuildFile();
        browse.waitForNode("lib");
        browse.doubleClickNode("lib");
        browse.waitForNode("junit-3.8.1.jar");
        browse.selectNode("junit-3.8.1.jar");
        browse.clickOkay();

        assertEquals("lib/junit-3.8.1.jar", antForm.getBuildFileFieldValue());
    }

    public void testBrowseSelectionOfScmDirectory() throws Exception
    {
        AntTypeForm antForm = insertTestSvnProjectAndNavigateToTypeConfig();
        assertTrue(antForm.isBrowseWorkingDirectoryLinkPresent());

        BrowseScmWindow browse = antForm.clickBrowseWorkingDirectory();
        browse.waitForNode("lib");
        browse.doubleClickNode("lib");
        assertFalse(browse.isNodePresent("junit-3.8.1.jar"));
        browse.selectNode("lib");
        browse.clickOkay();

        assertEquals("lib", antForm.getWorkingDirectoryFieldValue());
    }

    public void testBrowseAndCancelSelectionOfScmFile() throws Exception
    {
        AntTypeForm antForm = insertTestSvnProjectAndNavigateToTypeConfig();
        assertEquals("build.xml", antForm.getBuildFileFieldValue());
        assertTrue(antForm.isBrowseBuildFileLinkPresent());

        BrowseScmWindow browse = antForm.clickBrowseBuildFile();
        browse.waitForNode("lib");
        browse.doubleClickNode("lib");
        browse.waitForNode("junit-3.8.1.jar");
        browse.selectNode("junit-3.8.1.jar");
        browse.clickCancel();

        assertEquals("build.xml", antForm.getBuildFileFieldValue());
    }

    public void testBrowseFileUsesWorkingDirectory() throws Exception
    {
        AntTypeForm antForm = insertTestSvnProjectAndNavigateToTypeConfig();
        antForm.setFieldValue("work", "src");

        BrowseScmWindow browse = antForm.clickBrowseBuildFile();
        browse.waitForNode("java");
        assertFalse(browse.isNodePresent("lib"));
        assertFalse(browse.isNodePresent("build.xml"));
        assertTrue(browse.isNodePresent("test"));
        browse.expandPath("java", "com", "zutubi", "testant");
        browse.waitForNode("Unit.java");
        browse.selectNode("Unit.java");
        browse.clickOkay();

        assertEquals("java/com/zutubi/testant/Unit.java", antForm.getBuildFileFieldValue());
    }

    private AntTypeForm insertTestSvnProjectAndNavigateToTypeConfig() throws Exception
    {
        Hashtable<String, Object> svnConfig = xmlRpcHelper.getSubversionConfig(Constants.TEST_ANT_REPOSITORY);
        xmlRpcHelper.loginAsAdmin();
        xmlRpcHelper.insertProject(random, ProjectManager.GLOBAL_PROJECT_NAME, false, svnConfig, xmlRpcHelper.getAntConfig());
        xmlRpcHelper.logout();
        return navigateToTypeConfig();
    }

    private AntTypeForm insertTestGitProjectAndNavigateToTypeConfig() throws Exception
    {
        // the git repository is located on the local file system in the work.dir/git-repo directory
        File workingDir = AcceptanceTestUtils.getWorkingDirectory();
        File repositoryBase = new File(workingDir, "git-repo");

        Hashtable<String, Object> gitConfig = xmlRpcHelper.createEmptyConfig("zutubi.gitConfig");
        gitConfig.put("repository", "file://" + repositoryBase.getCanonicalPath());
        gitConfig.put("checkoutScheme", "CLEAN_CHECKOUT");
        gitConfig.put("monitor", false);
        xmlRpcHelper.loginAsAdmin();
        xmlRpcHelper.insertProject(random, ProjectManager.GLOBAL_PROJECT_NAME, false, gitConfig, xmlRpcHelper.getAntConfig());
        xmlRpcHelper.logout();
        return navigateToTypeConfig();
    }

    private AntTypeForm navigateToTypeConfig()
    {
        goTo(urls.adminProject(random) + "/" + Constants.Project.TYPE);
        AntTypeForm antForm = new AntTypeForm(selenium);
        antForm.waitFor();
        return antForm;
    }
}