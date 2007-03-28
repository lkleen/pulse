package com.zutubi.pulse.acceptance.forms;

import net.sourceforge.jwebunit.WebTester;

/**
 * <class-comment/>
 */
public class LdapConfigurationForm extends BaseForm
{
    public LdapConfigurationForm(WebTester tester)
    {
        super(tester);
    }

    public String getFormName()
    {
        return "ldap.config";
    }

    public String[] getFieldNames()
    {
        return new String[]{"ldap.enabled", "ldap.host", "ldap.baseDn", "ldap.managerDn", "ldap.managerPassword", "ldap.userBase", "ldap.userFilter", "ldap.autoAdd", "ldap.emailAttribute", "ldap.groupBaseDn", "ldap.groupFilter", "ldap.groupRoleAttribute", "ldap.groupSearchSubtree", "ldap.followReferrals", "ldap.escapeSpaces"};
    }

    public int[] getFieldTypes()
    {
        return new int[]{CHECKBOX, TEXTFIELD, TEXTFIELD, TEXTFIELD, TEXTFIELD, TEXTFIELD, TEXTFIELD, CHECKBOX, TEXTFIELD, TEXTFIELD, TEXTFIELD, TEXTFIELD, CHECKBOX, CHECKBOX, CHECKBOX};
    }
}
