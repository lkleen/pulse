package com.zutubi.pulse.acceptance;

import com.zutubi.pulse.acceptance.components.pulse.agent.AgentSummaryTable;
import com.zutubi.pulse.acceptance.pages.agents.AgentsPage;
import com.zutubi.pulse.acceptance.pages.browse.ProjectHomePage;
import com.zutubi.pulse.acceptance.utils.*;
import com.zutubi.pulse.acceptance.windows.PulseFileSystemBrowserWindow;
import com.zutubi.pulse.core.scm.config.api.CheckoutScheme;
import static com.zutubi.pulse.master.agent.AgentManager.MASTER_AGENT_NAME;
import com.zutubi.pulse.master.agent.AgentStatus;
import com.zutubi.pulse.master.tove.config.agent.AgentConfiguration;
import static com.zutubi.pulse.master.tove.config.agent.AgentConfigurationActions.ACTION_DISABLE;
import static com.zutubi.pulse.master.tove.config.agent.AgentConfigurationActions.ACTION_ENABLE;
import com.zutubi.pulse.master.tove.config.project.BootstrapConfiguration;
import com.zutubi.pulse.master.tove.config.project.ProjectConfiguration;
import static com.zutubi.pulse.master.tove.config.project.ProjectConfigurationWizard.DEFAULT_RECIPE;
import static com.zutubi.pulse.master.tove.config.project.ProjectConfigurationWizard.DEFAULT_STAGE;
import com.zutubi.pulse.master.tove.config.user.UserConfiguration;

/**
 * Tests for the working copy functionality.
 */
public class ProjectWorkingCopyAcceptanceTest extends AcceptanceTestBase
{
    private ConfigurationHelper configurationHelper;
    private ProjectConfigurations projects;
    private UserConfigurations users;
    private BuildRunner buildRunner;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        ConfigurationHelperFactory factory = new SingletonConfigurationHelperFactory();
        configurationHelper = factory.create(rpcClient.RemoteApi);

        buildRunner = new BuildRunner(rpcClient.RemoteApi);
        projects = new ProjectConfigurations(configurationHelper);
        users = new UserConfigurations();

        rpcClient.loginAsAdmin();
    }

    @Override
    protected void tearDown() throws Exception
    {
        rpcClient.logout();
        getBrowser().logout();

        super.tearDown();
    }

    public void testWorkingCopyLinkEnabledByIncrementalCheckoutScheme() throws Exception
    {
        ProjectConfiguration project = createProject(random);

        getBrowser().loginAsAdmin();

        ProjectHomePage homePage = getBrowser().openAndWaitFor(ProjectHomePage.class, random);
        assertFalse(homePage.isViewWorkingCopyPresent());

        updateCheckoutScheme(project, CheckoutScheme.INCREMENTAL_UPDATE);

        homePage.openAndWaitFor();
        assertTrue(homePage.isViewWorkingCopyPresent());
    }

    public void testWorkingCopyLinkControlledByViewSourcePermissions() throws Exception
    {
        ProjectConfiguration project = createProject(random);
        updateCheckoutScheme(project, CheckoutScheme.INCREMENTAL_UPDATE);

        getBrowser().loginAsAdmin();

        ProjectHomePage homePage = getBrowser().openAndWaitFor(ProjectHomePage.class, random);
        assertTrue(homePage.isViewWorkingCopyPresent());

        getBrowser().logout();

        // create user without view source permissions for the project and log in.
        UserConfiguration user = createUser(randomName());
        assertTrue(getBrowser().login(user.getName(), ""));

        homePage.openAndWaitFor();
        assertFalse(homePage.isViewWorkingCopyPresent());
    }

    public void testViewWorkingCopyOnMasterAgent() throws Exception
    {
        runTestViewWorkingCopyOnAgent(MASTER_AGENT_NAME);
    }

    public void testViewWorkingCopyOnRemoteAgent() throws Exception
    {
        rpcClient.RemoteApi.ensureAgent(AGENT_NAME);
        runTestViewWorkingCopyOnAgent(AGENT_NAME);
    }

    private void runTestViewWorkingCopyOnAgent(String agentName) throws Exception
    {
        ProjectConfiguration project = createProject(random, agentName);
        updateCheckoutScheme(project, CheckoutScheme.INCREMENTAL_UPDATE);
        buildRunner.triggerSuccessfulBuild(project);

        getBrowser().loginAsAdmin();

        ProjectHomePage homePage = getBrowser().openAndWaitFor(ProjectHomePage.class, random);
        assertTrue(homePage.isViewWorkingCopyPresent());

        PulseFileSystemBrowserWindow window = homePage.viewWorkingCopy();
        window.waitForLoadingToComplete();

        assertEquals("browse working copy", window.getHeader());
        assertTrue(window.isNodePresent("stage :: " + DEFAULT_STAGE + " :: [" + DEFAULT_RECIPE + "]@" + agentName));

        window.clickCancel();
        window.waitForClose();
        
        assertFalse(window.isWindowPresent());
    }

    public void testViewWorkingCopyBeforeFirstBuild() throws Exception
    {
        ProjectConfiguration project = createProject(random);
        updateCheckoutScheme(project, CheckoutScheme.INCREMENTAL_UPDATE);

        getBrowser().loginAsAdmin();

        ProjectHomePage homePage = getBrowser().openAndWaitFor(ProjectHomePage.class, random);
        assertTrue(homePage.isViewWorkingCopyPresent());

        PulseFileSystemBrowserWindow window = homePage.viewWorkingCopy();
        window.waitForLoadingToComplete();

        assertEquals("browse working copy", window.getHeader());
        assertFalse(window.isNodePresent("stage :: default :: [default]@master"));
        assertEquals("No build available", window.getStatus());

        window.clickCancel();
        window.waitForClose();
        assertFalse(window.isWindowPresent());
    }

    public void testViewWorkingCopyWhereAgentIsDisabled() throws Exception
    {
        rpcClient.RemoteApi.ensureAgent(AGENT_NAME);

        getBrowser().loginAsAdmin();
        enableAgent(AGENT_NAME);

        ProjectConfiguration project = createProject(random, AGENT_NAME);
        updateCheckoutScheme(project, CheckoutScheme.INCREMENTAL_UPDATE);
        buildRunner.triggerSuccessfulBuild(project);

        disableAgent(AGENT_NAME);

        ProjectHomePage homePage = getBrowser().openAndWaitFor(ProjectHomePage.class, random);
        assertTrue(homePage.isViewWorkingCopyPresent());

        PulseFileSystemBrowserWindow window = homePage.viewWorkingCopy();
        window.waitForLoadingToComplete();

        assertEquals("browse working copy", window.getHeader());
        assertEquals("", window.getStatus());

        String node = "stage :: default :: [default]@localhost";
        assertTrue(window.isNodePresent(node));
        window.doubleClickNode(node);
        window.waitForLoadingToComplete();
        
        assertEquals("Host not available", window.getStatus());

        enableAgent(AGENT_NAME);
    }

    private void disableAgent(String name) throws Exception
    {
        AgentsPage agentsPage = getBrowser().openAndWaitFor(AgentsPage.class);
        long agentId = rpcClient.TestApi.getAgentId(name);
        final AgentSummaryTable summaryTable = agentsPage.getAgentSummaryTable();
        if (summaryTable.areActionsAvailable(agentId, ACTION_DISABLE))
        {
            summaryTable.clickAction(agentId, ACTION_DISABLE);
            agentsPage.refreshUntilStatus(name, AgentStatus.DISABLED.getPrettyString());
        }
    }

    private void enableAgent(String name) throws Exception
    {
        AgentsPage agentsPage = getBrowser().openAndWaitFor(AgentsPage.class);
        long agentId = rpcClient.TestApi.getAgentId(name);
        final AgentSummaryTable summaryTable = agentsPage.getAgentSummaryTable();
        if (summaryTable.areActionsAvailable(agentId, ACTION_ENABLE))
        {
            summaryTable.clickAction(agentId, ACTION_ENABLE);
            agentsPage.refreshUntilStatus(name, AgentStatus.IDLE.getPrettyString());
        }
    }

    private ProjectConfiguration createProject(String projectName) throws Exception
    {
        return createProject(projectName, MASTER_AGENT_NAME);
    }

    private ProjectConfiguration createProject(String projectName, String targetAgentName) throws Exception
    {
        AgentConfiguration targetAgent = configurationHelper.getAgentReference(targetAgentName);

        AntProjectHelper project = projects.createTrivialAntProject(projectName);
        project.getStage(DEFAULT_STAGE).setAgent(targetAgent);
        configurationHelper.insertProject(project.getConfig(), false);
        return project.getConfig();
    }

    private UserConfiguration createUser(String name) throws Exception
    {
        UserConfiguration user = users.createSimpleUser(name);
        configurationHelper.insertUser(user);
        return user;
    }

    private void updateCheckoutScheme(ProjectConfiguration project, CheckoutScheme scheme) throws Exception
    {
        BootstrapConfiguration bootstrap = project.getBootstrap();
        bootstrap.setCheckoutScheme(scheme);
        configurationHelper.update(bootstrap);
    }
}
