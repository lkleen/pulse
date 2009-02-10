package com.zutubi.pulse.core.scm;

import com.zutubi.pulse.core.scm.api.ScmClient;
import com.zutubi.pulse.core.scm.api.ScmException;
import com.zutubi.pulse.core.scm.config.MockScmConfiguration;
import com.zutubi.util.bean.DefaultObjectFactory;
import com.zutubi.util.junit.ZutubiTestCase;

/**
 *
 *
 */
public class DelegateScmClientFactoryTest extends ZutubiTestCase
{
    private DelegateScmClientFactory factory;

    protected void setUp() throws Exception
    {
        super.setUp();

        factory = new DelegateScmClientFactory();
        factory.setObjectFactory(new DefaultObjectFactory());
        factory.register(MockScmConfiguration.class, MockScmClientFactory.class);
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testCreateClient() throws ScmException
    {
        ScmClient client = factory.createClient(new MockScmConfiguration());
        assertNotNull(client);
        try
        {
            assertTrue(client instanceof MockScmClient);
        }
        finally
        {
            client.close();
        }
    }

    public void testDataCache() throws ScmException
    {
        MockScmClient client = (MockScmClient) factory.createClient(new MockScmConfiguration());
        assertNotNull(client.cache);
        client.cache.put("key", "value");

        MockScmClient anotherClient = (MockScmClient) factory.createClient(new MockScmConfiguration());
        assertNotNull(anotherClient.cache);
        assertEquals("value", anotherClient.cache.get("key"));

        client.close();
        anotherClient.close();
    }
}