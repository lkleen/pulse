package com.zutubi.pulse.acceptance;

import com.zutubi.pulse.acceptance.forms.admin.ProjectAclForm;
import com.zutubi.pulse.acceptance.pages.admin.ListPage;
import com.zutubi.pulse.master.model.ProjectManager;
import com.zutubi.pulse.master.tove.config.MasterConfigurationRegistry;
import com.zutubi.tove.type.record.PathUtils;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItemInArray;

import java.util.Vector;

/**
 * Acceptance tests for project ACLs
 */
public class ProjectPermissionsAcceptanceTest extends AcceptanceTestBase
{
    protected void setUp() throws Exception
    {
        super.setUp();
        rpcClient.loginAsAdmin();
    }

    protected void tearDown() throws Exception
    {
        rpcClient.logout();
        super.tearDown();
    }

    public void testVisibilityOfGroups() throws Exception
    {
        rpcClient.RemoteApi.insertTrivialUser(random);

        String permissionsPath = PathUtils.getPath(MasterConfigurationRegistry.PROJECTS_SCOPE, ProjectManager.GLOBAL_PROJECT_NAME, "permissions");
        Vector<String> permissions = rpcClient.RemoteApi.getConfigListing(permissionsPath);

        assertTrue(getBrowser().login(random, ""));
        ListPage permissionsPage = getBrowser().openAndWaitFor(ListPage.class, permissionsPath);
        permissionsPage.clickAction(permissions.get(0), ListPage.ACTION_VIEW);

        ProjectAclForm aclForm = getBrowser().createForm(ProjectAclForm.class);
        aclForm.waitFor();
        String[] groups = aclForm.getComboBoxOptions("group");
        // Two options could just be the default plus the current value.  As
        // there are at least 3 groups, we can safely assert more than 2.
        assertTrue(groups.length > 2);
    }

    public void testPermissionLabels() throws Exception
    {
        String permissionsPath = PathUtils.getPath(MasterConfigurationRegistry.PROJECTS_SCOPE, ProjectManager.GLOBAL_PROJECT_NAME, "permissions");
        Vector<String> permissions = rpcClient.RemoteApi.getConfigListing(permissionsPath);

        getBrowser().loginAsAdmin();
        ListPage permissionsPage = getBrowser().openAndWaitFor(ListPage.class, permissionsPath);
        permissionsPage.clickAction(permissions.get(0), ListPage.ACTION_VIEW);

        ProjectAclForm aclForm = getBrowser().createForm(ProjectAclForm.class);
        aclForm.waitFor();
        String[] actionDisplays = aclForm.getComboBoxDisplays("allowedActions.choice");
        assertThat(actionDisplays, hasItemInArray("view"));
        assertThat(actionDisplays, hasItemInArray("cancel build"));
        assertThat(actionDisplays, hasItemInArray("view source"));

        String[] actionValues = aclForm.getComboBoxOptions("allowedActions.choice");
        assertThat(actionValues, hasItemInArray("view"));
        assertThat(actionValues, hasItemInArray("cancelBuild"));
        assertThat(actionValues, hasItemInArray("viewSource"));
    }
}
