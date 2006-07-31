package com.zutubi.pulse.core;

/**
 * <class-comment/>
 */
public class XCodePostProcessor extends PostProcessorGroup
{
    private String[] errorRegexs = new String[]
    {
            ".*error:.*",
            ".*Assertion failure.*",
            ".*No such file or directory.*",
            ".*Undefined symbols.*",
            ".*Uncaught exception:.*"
    };

    private String[] warningRegexs = new String[]
    {
            ".*warning:.*"
    };

    public XCodePostProcessor()
    {
        this(null);
    }

    public XCodePostProcessor(String name)
    {
        setName(name);

        // Regex for error patterns from xcode itself
        RegexPostProcessor xcode = new RegexPostProcessor();

        xcode.addErrorRegexs(errorRegexs);
        xcode.addWarningRegexs(warningRegexs);

        xcode.setLeadingContext(1);
        xcode.setTrailingContext(6);
        add(xcode);
    }
}
