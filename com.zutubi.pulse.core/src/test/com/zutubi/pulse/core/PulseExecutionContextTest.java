package com.zutubi.pulse.core;

import static com.zutubi.pulse.core.engine.api.BuildProperties.NAMESPACE_INTERNAL;
import com.zutubi.pulse.core.engine.api.ExecutionContext;
import com.zutubi.pulse.core.engine.api.Scope;
import com.zutubi.pulse.core.test.api.PulseTestCase;

import java.io.ByteArrayOutputStream;
import java.io.File;

/**
 */
public class PulseExecutionContextTest extends PulseTestCase
{
    public void testInternalProperty()
    {
        ExecutionContext context = new PulseExecutionContext();
        context.addString(NAMESPACE_INTERNAL, "foo", "bar");
        assertEquals("bar", context.getString(NAMESPACE_INTERNAL, "foo"));
        assertEquals("bar", context.getString("foo"));
    }

    public void testUserProperty()
    {
        ExecutionContext context = new PulseExecutionContext();
        context.addString("foo", "bar");
        assertNull(context.getString(NAMESPACE_INTERNAL, "foo"));
        assertEquals("bar", context.getString("foo"));
    }

    public void testGetBooleanNotSet()
    {
        ExecutionContext context = new PulseExecutionContext();
        assertFalse(context.getBoolean("foo", false));
        assertTrue(context.getBoolean("foo", true));
    }

    public void testGetBooleanBoolean()
    {
        ExecutionContext context = new PulseExecutionContext();
        context.addValue("foo", true);
        assertTrue(context.getBoolean("foo", false));
    }

    public void testGetBooleanString()
    {
        ExecutionContext context = new PulseExecutionContext();
        context.addValue("foo", "true");
        assertTrue(context.getBoolean("foo", false));
    }

    public void testGetBooleanOther()
    {
        ExecutionContext context = new PulseExecutionContext();
        context.addValue("foo", new Object());
        assertFalse(context.getBoolean("foo", false));
    }

    public void testGetLongNotSet()
    {
        ExecutionContext context = new PulseExecutionContext();
        assertEquals(0L, context.getLong("foo"));
    }

    public void testGetLongLong()
    {
        ExecutionContext context = new PulseExecutionContext();
        context.addValue("foo", 1L);
        assertEquals(1L, context.getLong("foo"));
    }

    public void testGetLongString()
    {
        ExecutionContext context = new PulseExecutionContext();
        context.addValue("foo", "33");
        assertEquals(33L, context.getLong("foo"));
    }

    public void testGetLongUnparseableString()
    {
        ExecutionContext context = new PulseExecutionContext();
        context.addValue("foo", "eek");
        assertEquals(0L, context.getLong("foo"));
    }

    public void testGetLongOther()
    {
        ExecutionContext context = new PulseExecutionContext();
        context.addValue("foo", new Object());
        assertEquals(0L, context.getLong("foo"));
    }

    public void testGetFileNotSet()
    {
        ExecutionContext context = new PulseExecutionContext();
        assertNull(context.getFile("foo"));
    }

    public void testGetFileFile()
    {
        ExecutionContext context = new PulseExecutionContext();
        context.addValue("foo", new File("abc"));
        assertEquals(new File("abc"), context.getFile("foo"));
    }

    public void testGetFileString()
    {
        ExecutionContext context = new PulseExecutionContext();
        context.addValue("foo", "xyz");
        assertEquals(new File("xyz"), context.getFile("foo"));
    }

    public void testGetFileOther()
    {
        ExecutionContext context = new PulseExecutionContext();
        context.addValue("foo", new Object());
        assertNull(context.getFile("foo"));
    }

    public void testCopy()
    {
        PulseExecutionContext context = makeNonTrivialContext();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        context.setOutputStream(outputStream);
        context.setVersion("ver");
        context.setWorkingDir(new File("foo"));

        PulseExecutionContext copy = new PulseExecutionContext(context);
        assertEquals("ip", copy.getString(NAMESPACE_INTERNAL, "iparent"));
        assertEquals("ic", copy.getString(NAMESPACE_INTERNAL, "ichild"));
        assertEquals("p", copy.getString("parent"));
        assertEquals("c", copy.getString("child"));
        assertSame(outputStream, context.getOutputStream());
        assertEquals("ver", copy.getVersion());
        assertEquals(new File("foo"), copy.getWorkingDir());
    }

    public void testCopyProducesUniqueScopes()
    {
        PulseExecutionContext context = makeNonTrivialContext();
        ExecutionContext copy = new PulseExecutionContext(context);
        for(Scope scopeCopy = copy.getScope(); scopeCopy != null; scopeCopy = scopeCopy.getParent())
        {
            for(Scope scope = context.getScope(); scope != null; scope = scope.getParent())
            {
                assertNotSame(scopeCopy, scope);
            }
        }
    }

    public void testGetScope()
    {
        PulseExecutionContext context = makeNonTrivialContext();

        PulseScope scope = context.getScope();
        assertEquals("ip", scope.getReferenceValue("iparent", String.class));
        assertEquals("ic", scope.getReferenceValue("ichild", String.class));
        assertEquals("p", scope.getReferenceValue("parent", String.class));
        assertEquals("c", scope.getReferenceValue("child", String.class));
    }

    private PulseExecutionContext makeNonTrivialContext()
    {
        PulseExecutionContext context = new PulseExecutionContext();
        context.addString(NAMESPACE_INTERNAL, "iparent", "ip");
        context.addString("parent", "p");
        context.push();
        context.addString(NAMESPACE_INTERNAL, "ichild", "ic");
        context.addString("child", "c");
        return context;
    }
}

