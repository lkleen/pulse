package com.zutubi.pulse.bootstrap.conf;

import com.zutubi.pulse.core.util.IOUtils;
import com.zutubi.pulse.test.PulseTestCase;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * <class-comment/>
 */
public class FileConfigTest extends PulseTestCase
{
    private Config config = null;
    private File testProperties;

    public FileConfigTest(String testName)
    {
        super(testName);
    }

    public void setUp() throws Exception
    {
        super.setUp();

        // temporary properties file
        testProperties = File.createTempFile(FileConfigTest.class.getName(), ".properties");

        Properties defaults = new Properties();
        defaults.put("key", "value");
        IOUtils.write(defaults, testProperties);

        // add setup code here.
        config = new FileConfig(testProperties);
    }

    public void tearDown() throws Exception
    {
        // add tear down code here.
        if (!testProperties.delete())
        {
            throw new IOException("");
        }

        config = null;

        super.tearDown();
    }

    public void testGetProperties() throws Exception
    {
        assertEquals("value", config.getProperty("key"));
    }

    public void testSetProperties() throws Exception
    {
        config.setProperty("key", "anotherValue");
        assertEquals("anotherValue", config.getProperty("key"));

        Properties props = IOUtils.read(testProperties);
        assertEquals("anotherValue", props.getProperty("key"));
        assertEquals(1, props.size());
    }

    public void testModifyPropertiesFileDirectly() throws Exception
    {
        assertEquals("value", config.getProperty("key"));

        // write updated properties directly to the properties file.
        Thread.sleep(1000);
        Properties updatedProperties = new Properties();
        updatedProperties.put("key", "anotherValue");
        IOUtils.write(updatedProperties, testProperties);

        // assert that the config has picked up these changes.
        assertEquals("anotherValue", config.getProperty("key"));
    }

    public void testCreationOfPropertiesFile() throws Exception
    {
        assertTrue(testProperties.delete());

        config.setProperty("key", "anotherValue");
        assertEquals("anotherValue", config.getProperty("key"));

        Properties props = IOUtils.read(testProperties);
        assertEquals("anotherValue", props.getProperty("key"));
        assertEquals(1, props.size());
    }
}
