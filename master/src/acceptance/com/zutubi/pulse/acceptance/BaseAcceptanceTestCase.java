package com.zutubi.pulse.acceptance;

import com.meterware.httpunit.*;
import com.zutubi.pulse.acceptance.forms.*;
import com.zutubi.pulse.util.Constants;
import junit.framework.Assert;
import org.apache.xmlrpc.XmlRpcClient;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Vector;

/**
 * <class-comment/>
 */
public abstract class BaseAcceptanceTestCase extends ExtendedWebTestCase
{
    protected static final String TEST_CVSROOT = ":ext:cvstester:cvs@www.cinnamonbob.com:/cvsroot";

    //---( add project wizard forms )---
    protected static final String FO_ANT_SETUP = "ant.setup";
    protected static final String FO_VERSIONED_SETUP = "versioned.setup";
    protected static final String VERSIONED_SETUP_FILE = "details.pulseFileName";

    protected String port;

    public BaseAcceptanceTestCase()
    {
    }

    public BaseAcceptanceTestCase(String name)
    {
        super(name);
    }

    protected void setUp() throws Exception
    {
        super.setUp();

        port = System.getProperty("pulse.port");
        if(port == null)
        {
            port = "8080";
        }

        getTestContext().setBaseUrl("http://localhost:" + port + "/");
    }

    protected void tearDown() throws Exception
    {
        tester = null;
        super.tearDown();
    }

    protected void login(String user, String password)
    {
        beginAt("/login.action");
        LoginForm loginForm = new LoginForm(tester);
        loginForm.loginFormElements(user, password, "false");
    }

    protected void loginAsAdmin()
    {
        beginAt("/login.action");
        LoginForm loginForm = new LoginForm(tester);
        loginForm.loginFormElements("admin", "admin", "false");
    }

    protected void logout()
    {
        clickLink(Navigation.LINK_LOGOUT);
    }

    protected void ensureLoggedOut()
    {
        if (isLinkPresent(Navigation.LINK_LOGOUT))
        {
            logout();
        }
    }

    protected boolean isLinkPresent(String linkId)
    {
        return tester.getDialog().isLinkPresent(linkId);
    }

    protected boolean hasLinkWithText(String text) throws Exception
    {
        return tester.getDialog().getResponse().getLinkWith(text) != null;
    }

    protected Object callRemoteApi(String function, Object... args) throws Exception
    {
        URL url = new URL("http", "127.0.0.1", Integer.valueOf(port), "/xmlrpc");
        XmlRpcClient client = new XmlRpcClient(url);
        Vector<Object> argVector = new Vector<Object>();

        // login as admin
        argVector.add("admin");
        argVector.add("admin");
        String token = (String) client.execute("RemoteApi.login", argVector);

        // Actual call
        argVector.clear();
        argVector.add(token);
        argVector.addAll(Arrays.asList(args));
        Object result = client.execute("RemoteApi." + function, argVector);

        // Logout
        argVector.clear();
        argVector.add(token);
        client.execute("RemoteApi.logout", argVector);

        return result;
    }

    /**
     * Assert that the cookie has been set as part of the conversation with the
     * server.
     *
     * @param cookieName
     */
    protected void assertCookieSet(String cookieName)
    {
        WebClient client = tester.getDialog().getWebClient();
        assertNotNull(client.getCookieValue(cookieName));
    }

    protected void assertCookieValue(String cookieName, String expectedValue)
    {
        WebClient client = tester.getDialog().getWebClient();
        assertEquals(expectedValue, client.getCookieValue(cookieName));
    }

    /**
     * Assert that the cookie has not been set as part of the conversation with
     * the server.
     *
     * @param cookieName
     */
    protected void assertCookieNotSet(String cookieName)
    {
        WebClient client = tester.getDialog().getWebClient();
        assertNull(client.getCookieValue(cookieName));
    }

    protected void submitCreateUserForm(String login, String name, String password, String confirm)
    {
        CreateUserForm form = new CreateUserForm(tester);
        form.assertFormPresent();
        form.saveFormElements(login, name, Boolean.toString(false), password, confirm);
    }

    protected void navigateToUserAdministration()
    {
        clickLink(Navigation.TAB_ADMINISTRATION);
        clickLink(Navigation.Administration.TAB_USERS);
    }

    protected void navigateToGroupsAdministration()
    {
        clickLink(Navigation.TAB_ADMINISTRATION);
        clickLink(Navigation.Administration.TAB_GROUPS);
    }

    protected void navigateToGeneralConfiguration()
    {
        clickLink(Navigation.TAB_ADMINISTRATION);
    }

    protected void navigateToJabberConfiguration()
    {
        clickLink(Navigation.TAB_ADMINISTRATION);
    }

    protected void navigateToLdapConfiguration()
    {
        clickLink(Navigation.TAB_ADMINISTRATION);
    }

    protected void navigateToSmtpConfiguration()
    {
        clickLink(Navigation.TAB_ADMINISTRATION);
    }

    protected void submitAntSetupForm()
    {
        assertFormPresent(FO_ANT_SETUP);
        setWorkingForm(FO_ANT_SETUP);
        submit("next");
    }

    protected void submitVersionedSetupForm(String file)
    {
        assertFormPresent(FO_VERSIONED_SETUP);
        setWorkingForm(FO_VERSIONED_SETUP);
        setFormElement(VERSIONED_SETUP_FILE, file);
        submit("next");
    }

    protected void submitCvsSetupForm(String root, String module, String password, String path)
    {
        CvsForm.Create form = new CvsForm.Create(tester);
        form.assertFormPresent();
        form.nextFormElements(root, module, password, "", "", "");
    }

    protected void submitProjectBasicsForm(String projectName, String description, String url, String scm, String type)
    {
        AddProjectWizard.Select form = new AddProjectWizard.Select(tester);
        form.assertFormPresent();
        form.nextFormElements(projectName, description, url, scm, type);
    }

    protected void ensureUser(String login) throws Exception
    {
        Vector<String> users = (Vector<String>) callRemoteApi("getAllUserLogins");
        if(!users.contains(login))
        {
            clickLink(Navigation.TAB_ADMINISTRATION);
            clickLink(Navigation.Administration.TAB_USERS);
            submitCreateUserForm(login, login, login, login);
        }
    }

    protected void ensureProject(String name) throws Exception
    {
        Vector<String> projects = (Vector<String>) callRemoteApi("getAllProjectNames");
        if(!projects.contains(name))
        {
            clickLink(Navigation.TAB_PROJECTS);
            clickLink(Navigation.Projects.LINK_ADD_PROJECT);
            submitProjectBasicsForm(name, "desc", "url", "cvs", "ant");
            submitCvsSetupForm(TEST_CVSROOT, "module", "", "");
            submitAntSetupForm();
        }
    }

    protected void ensureProjectGroup(String name, String... members) throws Exception
    {
        Vector<String> groups = (Vector<String>) callRemoteApi("getAllProjectGroups");
        if(!groups.contains(name))
        {
            Vector<String> v = new Vector<String>(members.length);
            for(String m: members)
            {
                v.add(m);
            }
            callRemoteApi("createProjectGroup", name, v);
        }
    }

    protected void createCommitMessageTransformer(String type, String... args)
    {
        clickLink(Navigation.TAB_ADMINISTRATION);

        // click the add link.
        assertAndClick("commit.message.transformer.add");

        // select a type.
        selectCommitMessageTransformerType(type);

        // fill in the blanks.
        BaseForm form = null;
        if (type.equals("link"))
        {
            form = new AddCommitMessageTransformerWizard.Link(tester);
        }
        else if (type.equals("custom"))
        {
            form = new AddCommitMessageTransformerWizard.Custom(tester);
        }
        else if (type.equals("jira"))
        {
            form = new AddCommitMessageTransformerWizard.Jira(tester);
        }

        form.assertFormPresent();
        form.finishFormElements(args);
        form.assertFormNotPresent();

        // ensure that the transformer is listed.
        assertLinkPresent("edit_" + args[0]);
    }

    protected void selectCommitMessageTransformerType(String type)
    {
        AddCommitMessageTransformerWizard.Select select = new AddCommitMessageTransformerWizard.Select(tester);
        select.assertFormPresent();
        select.nextFormElements(type);
        select.assertFormNotPresent();
    }

    public void assertAndClick(String name)
    {
        assertLinkPresent(name);
        clickLink(name);
    }

    public String getEditId(String name)
    {
        return "edit_" + name;
    }

    public boolean textInResponse(String text)
    {
        return tester.getDialog().isTextInResponse(text);
    }

    protected String getResponse() throws IOException
    {
        return tester.getDialog().getResponse().getText();
    }

    /**
     * An adaptation of the assertTableRowsEquals that allows us to assert a single row in a table.
     *
     * @param tableSummaryOrId
     * @param row
     * @param expectedValues
     */
    protected void assertTableRowEqual(String tableSummaryOrId, int row, String[] expectedValues)
    {
        assertTablePresent(tableSummaryOrId);
        String[][] sparseTableCellValues = tester.getDialog().getSparseTableBySummaryOrId(tableSummaryOrId);
        if (sparseTableCellValues.length <= row)
        {
            Assert.fail("Expected row["+row+"] does not exist. Actual number of rows is " + sparseTableCellValues.length);
        }
        for (int j = 0; j < expectedValues.length; j++) {
            if (expectedValues.length != sparseTableCellValues[row].length)
                Assert.fail("Unequal number of columns for row " + row + " of table " + tableSummaryOrId +
                        ". Expected [" + expectedValues.length + "] found [" + sparseTableCellValues[row].length + "].");
            String expectedString = expectedValues[j];
            Assert.assertEquals("Expected " + tableSummaryOrId + " value at [" + row + "," + j + "] not found.",
                    expectedString, tester.getTestContext().toEncodedString(sparseTableCellValues[row][j].trim()));
        }
    }

    protected void goTo(String relativeUrl) throws HttpException, IOException, SAXException
    {
        String baseUrl = getTestContext().getBaseUrl();

        String targetUrl = "";
        if (baseUrl.endsWith("/") && relativeUrl.startsWith("/"))
        {
            targetUrl = baseUrl + relativeUrl.substring(1);
        }
        else
        {
            targetUrl = baseUrl + relativeUrl;
        }

        tester.getDialog().getWebClient().getResponse(new GetMethodWebRequest(targetUrl));
    }

    protected void pauseWhileMetaRefreshActive() throws InterruptedException, IOException, SAXException
    {
        int delay = 0;
        while ((delay = tester.getDialog().getResponse().getRefreshDelay()) > 0)
        {
            Thread.sleep(delay * Constants.SECOND);
            WebRequest req = tester.getDialog().getResponse().getRefreshRequest();
            WebResponse resp = tester.getDialog().getWebClient().getResponse(req);
        }
    }

}