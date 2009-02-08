package com.zutubi.pulse.acceptance.forms;

import junit.framework.Assert;
import net.sourceforge.jwebunit.WebTester;

/**
 * <class-comment/>
 */
public abstract class BaseForm
{
    protected final WebTester tester;
    protected static final int TEXTFIELD = 3;
    protected static final int CHECKBOX = 4;
    protected static final int RADIOBOX = 5;
    protected static final int SELECT = 6;
    protected static final int MULTI_CHECKBOX = 7;
    protected static final int MULTI_SELECT = 8;

    public BaseForm(WebTester tester)
    {
        this.tester = tester;
    }

    public void assertFormPresent()
    {
        tester.assertFormPresent(getFormName());
        String[] names = getFieldNames();
        for (String name : names)
        {
            String[] selectValues = getSelectOptions(name);
            if (selectValues != null)
            {
                tester.assertOptionsEqual(name, selectValues);
            }
        }
    }

    public void assertFormNotPresent()
    {
        tester.assertFormNotPresent(getFormName());
    }

    public abstract String getFormName();

    public abstract String[] getFieldNames();

    /**
     * Returns the type identifiers for the form fields. The default implementation
     * returns an array of TEXTFIELD identifiers.
     *
     * @return an array of form field identifiers.
     * @see BaseForm#TEXTFIELD
     * @see BaseForm#CHECKBOX
     * @see BaseForm#RADIOBOX
     * @see BaseForm#SELECT
     */
    public int[] getFieldTypes()
    {
        int[] types = new int[getFieldNames().length];
        for (int i = 0; i < types.length; i++)
        {
            types[i] = TEXTFIELD;
        }
        return types;
    }

    public String[] getSelectOptions(String name)
    {
        setActive();
        return tester.getDialog().getOptionsFor(name);
    }

    public void addFormElements(String... args)
    {
        setFormElements(args);
        tester.submit("add");
    }

    public void saveFormElements(String... args)
    {
        setFormElements(args);
        save();
    }

    public void save()
    {
        tester.submit("save");
    }

    public void nextFormElements(String... args)
    {
        setFormElements(args);
        next();
    }

    public void finishFormElements(String... args)
    {
        setFormElements(args);
        tester.submit("finish");
    }

    public void next()
    {
        tester.submit("next");
    }

    public void previous()
    {
        tester.submit("previous");
    }

    public void cancel()
    {
        tester.submit("cancel");
    }

    public void cancelFormElements(String... args)
    {
        setFormElements(args);
        cancel();
    }

    /**
     * @param values
     */
    public void setFormElements(String... values)
    {
        tester.assertFormPresent(getFormName());
        tester.setWorkingForm(getFormName());

        int[] types = getFieldTypes();
        Assert.assertEquals(values.length, types.length);

        for (int i = 0; i < types.length; i++)
        {
            switch (types[i])
            {
                case TEXTFIELD:
                case SELECT:
                    if (values[i] != null)
                    {
                        tester.setFormElement(getFieldNames()[i], values[i]);
                    }
                    break;
                case CHECKBOX:
                    setCheckboxChecked(getFieldNames()[i], Boolean.valueOf(values[i]));
                    break;
                case RADIOBOX:
                    if (values[i] != null)
                    {
                        setRadioboxSelected(getFieldNames()[i], values[i]);
                    }
                    break;
                case MULTI_CHECKBOX:
                case MULTI_SELECT:
                    if (values[i] != null)
                    {
                        setMultiValues(getFieldNames()[i], values[i]);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public void assertFormElements(String... values)
    {
        tester.assertFormPresent(getFormName());
        tester.setWorkingForm(getFormName());

        int[] types = getFieldTypes();
        Assert.assertEquals(values.length, types.length);

        for (int i = 0; i < types.length; i++)
        {
            switch (types[i])
            {
                case TEXTFIELD:
                    tester.assertFormElementEquals(getFieldNames()[i], values[i]);
                    break;
                case CHECKBOX:
                    assertCheckboxChecked(getFieldNames()[i], Boolean.valueOf(values[i]));
                    break;
                case RADIOBOX:
                    assertRadioboxSelected(getFieldNames()[i], values[i]);
                    break;
                case SELECT:
                    // Can set to null to ignore (e.g. multiselect where this doesn't work)
                    if (values[i] != null)
                    {
                        tester.assertFormElementEquals(getFieldNames()[i], values[i]);
                    }
                    break;
                case MULTI_CHECKBOX:
                case MULTI_SELECT:
                    if (values[i] != null)
                    {
                        String[] expected;

                        if (values[i].length() > 0)
                        {
                            expected = values[i].split(",");
                        }
                        else
                        {
                            expected = new String[0];
                        }

                        assertMultiValues(getFieldNames()[i], expected);
                    }
                default:
                    break;
            }
        }
    }

    public void assertFormReset()
    {
        assertFormElements(getDefaultValues());
    }

    public String[] getDefaultValues()
    {
        int[] types = getFieldTypes();

        String[] defaultValues = new String[types.length];
        for (int i = 0; i < types.length; i++)
        {
            defaultValues[i] = "";
        }
        return defaultValues;
    }

    public String[] getFormValues()
    {
        String[] fieldNames = getFieldNames();
        String[] formValues = new String[fieldNames.length];
        for (int i = 0; i < fieldNames.length; i++)
        {
            String fieldName = fieldNames[i];
            formValues[i] = tester.getDialog().getFormParameterValue(fieldName);
        }
        return formValues;
    }

    public String getOptionValue(String field, String option)
    {
        return tester.getDialog().getValueForOption(field, option);
    }

    public void assertOptionNotPresent(String field, String option)
    {
        for (String o : tester.getDialog().getOptionsFor(field))
        {
            if (o.equals(option))
            {
                Assert.fail("Unexpected option '" + option + "' present in field '" + field + "'");
            }
        }
    }

    public void assertMultiValues(String name, String... values)
    {
        String[] gotValues = tester.getDialog().getForm().getParameterValues(name);
        Assert.assertEquals(values.length, gotValues.length);
        for (int i = 0; i < values.length; i++)
        {
            Assert.assertEquals(values[i], gotValues[i]);
        }
    }

    public void setMultiValues(String name, String values)
    {
        String[] set;
        if (values.length() > 0)
        {
            set = values.split(",");
        }
        else
        {
            set = new String[0];
        }

        tester.getDialog().getForm().setParameter(name, set);
    }

    public void setRadioboxSelected(String fieldName, String selectedOption)
    {
        tester.setFormElement(fieldName, selectedOption);
    }

    public void assertRadioboxSelected(String fieldName, String option)
    {
        tester.assertRadioOptionSelected(fieldName, option);
    }

    public void setCheckboxChecked(String name, boolean b)
    {
        if (b)
        {
            tester.setFormElement(name, "true");
        }
        else
        {
            tester.uncheckCheckbox(name);
        }
    }

    public void assertCheckboxChecked(String name, boolean b)
    {
        if (b)
        {
            tester.assertFormElementEquals(name, "true");
        }
        else
        {
            tester.assertFormElementEquals(name, null);
        }
    }

    public void assertCheckboxChecked(String name)
    {
        assertCheckboxChecked(name, true);
    }

    public void assertCheckboxNotChecked(String name)
    {
        assertCheckboxChecked(name, false);
    }

    public void setActive()
    {
        tester.setWorkingForm(getFormName());
    }
}