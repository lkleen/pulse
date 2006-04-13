/********************************************************************************
 @COPYRIGHT@
 ********************************************************************************/
package com.zutubi.pulse.acceptance;

import com.zutubi.pulse.acceptance.forms.JabberConfigurationForm;

/**
 * <class-comment/>
 */
public class JabberConfigurationAcceptanceTest extends BaseAcceptanceTest
{
    public JabberConfigurationAcceptanceTest()
    {
    }

    public JabberConfigurationAcceptanceTest(String name)
    {
        super(name);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        login("admin", "admin");
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }


    private void navigateToJabberConfiguration()
    {
        beginAt("/");
        clickLinkWithText("administration");
    }

    public void testEdit() throws Exception
    {
        navigateToJabberConfiguration();

        JabberConfigurationForm form = new JabberConfigurationForm(tester);

        assertAndClick("jabber.edit");
        form.assertFormPresent();

        form.saveFormElements("testhost", "10", "testuser", "testpassword");
        assertJabberTable("testhost", "10", "testuser");
    }

    public void testReset() throws Exception
    {
        navigateToJabberConfiguration();
        testEdit();

        assertAndClick("jabber.reset");
        assertJabberTable("", "5222", "");
    }

    public void testCancel() throws Exception
    {
        navigateToJabberConfiguration();

        assertAndClick("jabber.edit");
        JabberConfigurationForm form = new JabberConfigurationForm(tester);
        form.assertFormPresent();
        form.cancelFormElements("boohoo", "1", "hoo", "hoo");

        assertTextNotPresent("boohoo");
    }

    public void testValidate() throws Exception
    {
        navigateToJabberConfiguration();

        assertAndClick("jabber.edit");
        JabberConfigurationForm form = new JabberConfigurationForm(tester);
        form.assertFormPresent();
        form.saveFormElements("", "-1", "", "");
        form.assertFormPresent();
        assertTextPresent("jabber server is required");
        assertTextPresent("port must be a positive integer");
        assertTextPresent("username is required");
    }

    private void assertJabberTable(String host, String port, String username)
    {
        assertTablePresent("jabber.config");
        assertTableRowsEqual("jabber.config", 1, new String[][] {{"jabber server", host}, {"port", port}, {"username",username}});
    }
}
