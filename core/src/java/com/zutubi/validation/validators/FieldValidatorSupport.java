package com.zutubi.validation.validators;

import com.zutubi.validation.FieldValidator;
import com.zutubi.validation.ValidationContext;

/**
 * <class-comment/>
 */
public abstract class FieldValidatorSupport extends ValidatorSupport implements FieldValidator
{
    private String fieldName;

    private ValidationContext context;

    private String defaultMessage;

    private String messageKey;
    private String defaultMessageKey;

    public String getFieldName()
    {
        return fieldName;
    }

    public void setFieldName(String fieldName)
    {
        this.fieldName = fieldName;
    }

    protected Object[] getMessageArgs()
    {
        return new Object[]{getFieldName()};
    }

    protected String getMessage()
    {
        // just a bit of craziness...
        String message;

        if (messageKey != null)
        {
            messageKey = messageKey.replace("${fieldName}", getFieldName());
            message = validationContext.getText(messageKey, getMessageArgs());
            if (message == null)
            {
                message = determineDefaultMessage();
            }
        }
        else
        {
            message = determineDefaultMessage();
        }

        if (message == null)
        {
            message = "no.message.available";
        }
        return message;
    }

    private String determineDefaultMessage()
    {
        if (defaultMessage != null)
        {
            return defaultMessage;
        }
        else
        {
            if (defaultMessageKey != null)
            {
                defaultMessageKey = defaultMessageKey.replace("${fieldName}", getFieldName());
                defaultMessage = validationContext.getText(defaultMessageKey, getMessageArgs());
            }
            if (defaultMessage == null)
            {
                defaultMessage = messageKey;
            }
            return defaultMessage;
        }
    }

    public String getDefaultMessage()
    {
        return defaultMessage;
    }

    public void setDefaultMessage(String defaultMessage)
    {
        this.defaultMessage = defaultMessage;
    }

    public String getMessageKey()
    {
        return messageKey;
    }

    public void setMessageKey(String messageKey)
    {
        this.messageKey = messageKey;
    }

    protected void setDefaultMessageKey(String messageKey)
    {
        this.defaultMessageKey = messageKey;
    }

    // helper methods

    protected void addFieldError(String fieldName)
    {
        validationContext.addFieldError(fieldName, getMessage());
    }

    protected void addActionError()
    {
        validationContext.addActionError(getMessage());
    }
}
