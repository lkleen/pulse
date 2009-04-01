package com.zutubi.pulse.core.dependency.ivy;

import com.zutubi.validation.FieldValidator;
import com.zutubi.validation.ValidationException;
import com.zutubi.validation.validators.FieldValidatorTestCase;

public class IvyPatternValidatorTest extends FieldValidatorTestCase
{
    public IvyPatternValidatorTest(String testName)
    {
        super(testName);
    }

    protected FieldValidator createValidator()
    {
        return new IvyPatternValidator();
    }

    public void testValidPattern() throws Exception
    {
        validator.validate(new FieldProvider("[artifact]"));
        assertFalse(validationAware.hasErrors());
    }

    public void testIncompletePattern() throws ValidationException
    {
        validator.validate(new FieldProvider("[artifact"));
        assertTrue(validationAware.hasErrors());
    }

    public void testInvalidPattern() throws ValidationException
    {
        validator.validate(new FieldProvider("[[artifact]"));
        assertTrue(validationAware.hasErrors());
    }

    public void testPulsePropertyReference() throws ValidationException
    {
        validator.validate(new FieldProvider("${something}"));
        assertFalse(validationAware.hasErrors());
    }

}