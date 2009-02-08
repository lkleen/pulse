package com.zutubi.pulse.core;

import com.zutubi.pulse.core.model.*;
import com.zutubi.pulse.test.PulseTestCase;
import com.zutubi.pulse.util.FileSystemUtils;

import java.io.File;

/**
 */
public abstract class XMLReportPostProcessorTestBase extends PulseTestCase
{
    protected XMLReportPostProcessor pp;

    public XMLReportPostProcessorTestBase(XMLReportPostProcessor pp)
    {
        this.pp = pp;
    }

    public XMLReportPostProcessorTestBase(String name, XMLReportPostProcessor pp)
    {
        super(name);
        this.pp = pp;
    }

    protected void setUp() throws Exception
    {
        super.setUp();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    protected File getOutputDir()
    {
        File root = getPulseRoot();
        return new File(root, FileSystemUtils.composeFilename("core", "src", "test", "com", "zutubi", "pulse", "core"));
    }

    protected StoredFileArtifact getArtifact(String name)
    {
        return new StoredFileArtifact(getClass().getSimpleName() + "." + name + ".xml");
    }

    protected TestSuiteResult runProcessor(String... names)
    {
        File outputDir = getOutputDir();
        TestSuiteResult testResults = new TestSuiteResult();
        CommandContext context = new CommandContext(null, outputDir, testResults);

        for(String name: names)
        {
            StoredFileArtifact artifact = getArtifact(name);
            pp.process(artifact, new CommandResult("test"), context);
        }
        
        return testResults;
    }

    protected void checkCase(TestCaseResult caseResult, String name, TestCaseResult.Status status, long duration, String message)
    {
        assertEquals(name, caseResult.getName());
        assertEquals(status, caseResult.getStatus());
        assertEquals(duration, caseResult.getDuration());
        assertEquals(message, caseResult.getMessage());
    }

    protected void checkCase(TestCaseResult caseResult, String name, TestCaseResult.Status status, String message)
    {
        checkCase(caseResult, name, status, TestResult.UNKNOWN_DURATION, message);
    }

    protected void checkSuite(TestSuiteResult suite, String name, int total, int failures, int errors)
    {
        assertEquals(name, suite.getName());
        assertEquals(total, suite.getTotal());
        assertEquals(failures, suite.getFailures());
        assertEquals(errors, suite.getErrors());
    }

    protected void checkPassCase(TestSuiteResult suite, String name)
    {
        TestCaseResult caseResult = suite.getCase(name);
        assertNotNull(caseResult);
        checkPassCase(caseResult, name);
    }

    protected void checkPassCase(TestCaseResult caseResult, String name)
    {
        checkCase(caseResult, name, TestCaseResult.Status.PASS, null);
    }

    protected void checkFailureCase(TestSuiteResult suite, String name, String message)
    {
        TestCaseResult caseResult = suite.getCase(name);
        assertNotNull(caseResult);
        checkFailureCase(caseResult, name, message);
    }

    protected void checkFailureCase(TestCaseResult caseResult, String name, String message)
    {
        checkCase(caseResult, name, TestCaseResult.Status.FAILURE, message);
    }

    protected void checkErrorCase(TestSuiteResult suite, String name, String message)
    {
        TestCaseResult caseResult = suite.getCase(name);
        assertNotNull(caseResult);
        checkErrorCase(caseResult, name, message);
    }

    protected void checkErrorCase(TestCaseResult caseResult, String name, String message)
    {
        checkCase(caseResult, name, TestCaseResult.Status.ERROR, message);
    }
}
