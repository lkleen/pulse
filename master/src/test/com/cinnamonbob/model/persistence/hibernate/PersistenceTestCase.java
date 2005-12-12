package com.cinnamonbob.model.persistence.hibernate;

import com.cinnamonbob.bootstrap.ComponentContext;
import com.cinnamonbob.bootstrap.DatabaseBootstrap;
import com.cinnamonbob.test.BobTestCase;
import org.hibernate.SessionFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.sql.DataSource;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.Statement;

/**
 * 
 *
 */
public abstract class PersistenceTestCase extends BobTestCase
{
    protected ApplicationContext context = null;

    private PlatformTransactionManager transactionManager;
    private TransactionStatus transactionStatus;
    private DefaultTransactionDefinition transactionDefinition;

    protected SessionFactory sessionFactory;

    public PersistenceTestCase()
    {

    }

    public PersistenceTestCase(String testName)
    {
        super(testName);
    }

    public void setUp() throws Exception
    {
        super.setUp();

        String[] configLocations = getConfigLocations();

        ComponentContext.addClassPathContextDefinitions(configLocations);
        context = ComponentContext.getContext();

        DataSource dataSource = (DataSource) context.getBean("dataSource");

        DatabaseBootstrap dbBootstrap = new DatabaseBootstrap();
        dbBootstrap.setDataSource(dataSource);
        dbBootstrap.initialiseDatabase();

        transactionManager = (PlatformTransactionManager)context.getBean("transactionManager");
        transactionDefinition = new DefaultTransactionDefinition(DefaultTransactionDefinition.PROPAGATION_REQUIRED);
        transactionStatus = transactionManager.getTransaction(transactionDefinition);

        sessionFactory = (SessionFactory) context.getBean("sessionFactory");
    }

    public void tearDown() throws Exception
    {
        sessionFactory = null;

        try
        {
            transactionManager.commit(transactionStatus);
        }
        catch (Exception e)
        {
            // catch the exception and continue.
            e.printStackTrace();
        }

        Connection con = ((DataSource)context.getBean("dataSource")).getConnection();
        Statement stmt = null;
        try
        {
            stmt = con.createStatement();
            stmt.execute("SHUTDOWN");
        }
        finally
        {
            if (stmt != null)
                stmt.close();
        }
        transactionStatus = null;
        transactionDefinition = null;
        transactionManager = null;
        super.tearDown();
    }

    protected String[] getConfigLocations()
    {
        return new String[]{
            "com/cinnamonbob/bootstrap/testBootstrapContext.xml",
            "com/cinnamonbob/bootstrap/testApplicationContext.xml"
        };
    }

    protected void commitAndRefreshTransaction()
    {
        transactionManager.commit(transactionStatus);
        transactionStatus = transactionManager.getTransaction(transactionDefinition);
    }

    protected void assertPropertyEquals(Object a, Object b)
    {
        try
        {
            BeanInfo beanInfo = Introspector.getBeanInfo(a.getClass());
            for (PropertyDescriptor property : beanInfo.getPropertyDescriptors())
            {
                Method getter = property.getReadMethod();
                if (getter.getDeclaringClass() != Object.class)
                {
                    assertObjectEquals(getter.getName(), getter.invoke(a), getter.invoke(b));
                }
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
