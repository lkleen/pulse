package com.zutubi.pulse.core;

import com.zutubi.pulse.core.test.api.PulseTestCase;

/**
 * <class-comment/>
 */
public class ResourceReferenceTest extends PulseTestCase
{
    public void setUp() throws Exception
    {

    }

    public void tearDown() throws Exception
    {

    }

    public void testResourceReference() throws Exception
    {
        PulseScope parent = new PulseScope();
        PulseScope scope = new PulseScope(parent);
        FileResourceRepository repo = ResourceFileLoader.load(getInputFile("testResourceReference", "xml"));
        ResourceReference ref = new ResourceReference();
        ref.setResourceRepository(repo);

        ref.setScope(scope);
        ref.setName("aResource");
        ref.setVersion("aVersion");
        ref.initBeforeChildren();

        assertTrue(parent.containsReference("b"));
        assertTrue(parent.containsReference("d"));
        assertEquals("c", parent.getReference("b").getValue());
        assertEquals("e", parent.getReference("d").getValue());

        assertFalse(parent.containsReference("1"));
        assertFalse(parent.containsReference("3"));
    }
}
