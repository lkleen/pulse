package com.zutubi.pulse.acceptance.cleanup;

import com.zutubi.pulse.acceptance.forms.admin.CleanupForm;
import com.zutubi.pulse.acceptance.pages.admin.CleanupRulesPage;
import com.zutubi.pulse.acceptance.pages.admin.ProjectConfigPage;
import com.zutubi.pulse.acceptance.SeleniumTestBase;
import static com.zutubi.util.CollectionUtils.asPair;

/**
 * The acceptance tests that run through the cleanup web ui.
 */
public class CleanupUIAcceptanceTest extends SeleniumTestBase
{
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

         xmlRpcHelper.loginAsAdmin();
    }

    @Override
    protected void tearDown() throws Exception
    {
        xmlRpcHelper.logout();

        super.tearDown();
    }

    public void testDefaultCleanupRulePresent()
    {
        addProject(random, true);

        loginAsAdmin();
        
        ProjectConfigPage projectPage = new ProjectConfigPage(selenium, urls, random, false);
        projectPage.goTo();        

        CleanupRulesPage cleanupRulesPage = projectPage.clickCleanupAndWait();
        cleanupRulesPage.clickView("default");

        CleanupForm cleanup = new CleanupForm(selenium);
        cleanup.waitFor();
        assertEquals("default", cleanup.getFieldValue("name"));
        assertEquals("WORKING_COPY_SNAPSHOT", cleanup.getFieldValue("what"));
        assertEquals("10", cleanup.getFieldValue("retain"));
        assertEquals("BUILDS", cleanup.getFieldValue("unit"));
    }

    public void testCreateNewCleanupRule()
    {
        addProject(random, true);

        loginAsAdmin();

        ProjectConfigPage projectPage = new ProjectConfigPage(selenium, urls, random, false);
        projectPage.goTo();

        CleanupRulesPage cleanupRulesPage = projectPage.clickCleanupAndWait();
        cleanupRulesPage.clickAdd();

        CleanupForm cleanup = new CleanupForm(selenium);
        cleanup.waitFor();
        cleanup.finishNamedFormElements(asPair("name", "new rule"), asPair("retain", "1"), asPair("cleanupAll", "true"));

        cleanupRulesPage.goTo();
        cleanupRulesPage.waitFor();
        assertTrue(cleanupRulesPage.isItemPresent("new rule"));
    }
}
