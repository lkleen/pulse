package com.zutubi.pulse.acceptance.forms;

import net.sourceforge.jwebunit.WebTester;

/**
 * <class-comment/>
 */
public class VersionedProjectEditForm extends BaseForm
{
    public static String FORM_NAME = "versioned.edit";
    public static String FIELD_FILE = "details.pulseFileName";

    public VersionedProjectEditForm(WebTester tester)
    {
        super(tester);
    }

    public String getFormName()
    {
        return FORM_NAME;
    }

    public String[] getFieldNames()
    {
        return new String[]{FIELD_FILE};
    }
}