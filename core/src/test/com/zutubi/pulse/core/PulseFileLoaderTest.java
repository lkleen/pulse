package com.zutubi.pulse.core;

import com.zutubi.pulse.test.PulseTestCase;
import com.zutubi.pulse.model.ResourceRequirement;
import com.zutubi.pulse.util.IOUtils;

import java.util.LinkedList;
import java.util.List;

/**
 */
public class PulseFileLoaderTest extends PulseTestCase
{
    PulseFileLoader loader;

    public void setUp()
    {
        loader = new PulseFileLoader(new ObjectFactory());
    }

    protected void tearDown() throws Exception
    {
        loader = null;
    }

    public void testLoaderRequireResources() throws Exception
    {
        List<ResourceRequirement> requriements = loader.loadRequiredResources(IOUtils.inputStreamToString(getInput("requiredResources")), null);

        assertEquals(2, requriements.size());
        assertEquals("noversion", requriements.get(0).getResource());
        assertNull(requriements.get(0).getVersion());
        assertEquals("withversion", requriements.get(1).getResource());
        assertEquals("1", requriements.get(1).getVersion());
    }
}
