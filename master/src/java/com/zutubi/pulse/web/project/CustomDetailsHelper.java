package com.zutubi.pulse.web.project;

import com.opensymphony.util.TextUtils;
import com.opensymphony.xwork.ValidationAware;
import com.zutubi.pulse.core.*;
import com.zutubi.pulse.model.CustomProjectValidationPredicate;
import com.zutubi.pulse.util.StringUtils;

import java.io.ByteArrayInputStream;

/**
 */
public class CustomDetailsHelper
{
    private int lineNumber;
    private String line;
    private int lineOffset;

    public int getLineNumber()
    {
        return lineNumber;
    }

    public String getLine()
    {
        return line;
    }

    public int getLineOffset()
    {
        return lineOffset;
    }

    public void validate(ValidationAware action, String pulseFile, ResourceRepository resourceRepository)
    {
        if(!TextUtils.stringSet(pulseFile))
        {
            action.addFieldError("details.pulseFile", "pulse file is required");
            return;
        }

        try
        {
            PulseFileLoader loader = new PulseFileLoader();
            loader.setObjectFactory(new ObjectFactory());
            
            loader.load(new ByteArrayInputStream(pulseFile.getBytes()), new PulseFile(), new Scope(), resourceRepository, new CustomProjectValidationPredicate());
        }
        catch(ParseException pe)
        {
            action.addActionError(pe.getMessage());
            if(pe.getLine() > 0)
            {
                line = StringUtils.getLine(pulseFile, pe.getLine());
                if(line != null)
                {
                    lineNumber = pe.getLine();
                    lineOffset = StringUtils.getLineOffset(pulseFile, lineNumber);
                    action.addActionError("First line of offending element: " + line);
                }
            }
        }
        catch(Exception e)
        {
            action.addActionError(e.getMessage());
        }
    }
}