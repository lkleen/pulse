/* Copyright 2017 Zutubi Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zutubi.pulse.master.database;

import com.google.common.io.ByteStreams;
import com.zutubi.pulse.core.test.api.PulseTestCase;
import com.zutubi.util.io.FileSystemUtils;
import com.zutubi.util.io.IOUtils;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;

public class DriverRegistryTest extends PulseTestCase
{
    private File tmp;
    private DriverRegistry registry;

    private static final String EMBEDDED_DRIVER = "org.hsqldb.jdbcDriver";
    private static final String POSTGRESQL_DRIVER = "org.postgresql.Driver";

    private File postgresqlJar;
    private File driverDir;

    protected void setUp() throws Exception
    {
        super.setUp();

        tmp = createTempDirectory();
        driverDir = new File(tmp, "drivers");

        // copy the postgresql jar from the classpath into the temporary directory.
        ClassPathResource resource = new ClassPathResource("com/zutubi/pulse/master/database/lib/postgresql.jar");
        postgresqlJar = new File(tmp, "postgresql.jar");

        ByteStreams.copy(resource.getInputStream(), new FileOutputStream(postgresqlJar));

        registry = new DriverRegistry();
        registry.setDriverDir(driverDir);
        registry.init();
    }

    protected void tearDown() throws Exception
    {
        unregisterAllDriversFromDriverManager();

        try
        {
            FileSystemUtils.rmdir(tmp);
        }
        catch (IOException e)
        {
            // By loading the classfile from the postgresql jar, we cause problems with the
            // deletion of that file, someone keeps an open file handle.
            tmp = null;
            driverDir = null;
        }

        super.tearDown();
    }

    private void unregisterAllDriversFromDriverManager() throws SQLException
    {
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements())
        {
            DriverManager.deregisterDriver(drivers.nextElement());
        }
    }

    public void testRegisterExternalDriver() throws IOException
    {
        registry.register(POSTGRESQL_DRIVER, postgresqlJar);

        assertTrue(registry.isRegistered(POSTGRESQL_DRIVER));

        // verify that we can use the driver as expected.
        try
        {
            DriverManager.getDriver("jdbc:postgresql://localhost:5432/blah");
        }
        catch (SQLException e)
        {
            assertFalse(e.getMessage().contains("No suitable driver"));
        }

    }

    public void testRegisterEmbeddedDriver() throws IOException
    {
        registry.register(EMBEDDED_DRIVER);

        assertTrue(registry.isRegistered(EMBEDDED_DRIVER));

        try
        {
            DriverManager.getDriver("jdbc:hsqldb:C:/some/path/to/db");
        }
        catch (SQLException e)
        {
            assertFalse(e.getMessage().contains("No suitable driver"));
        }
    }

    public void testRegistryFileContents() throws IOException
    {
        // check file is there.
        File expectedRegistryFile = new File(driverDir, ".registry");
        assertTrue(expectedRegistryFile.isFile());

        assertFalse(IOUtils.read(expectedRegistryFile).containsKey(EMBEDDED_DRIVER));

        registry.register(EMBEDDED_DRIVER);

        assertTrue(IOUtils.read(expectedRegistryFile).containsKey(EMBEDDED_DRIVER));
    }

    public void testBadDriver() throws IOException
    {
        try
        {
            registry.register("org.unknown.jdbcDriver");
            fail();
        }
        catch (IOException e)
        {
            assertEquals("Unable to load database driver: org.unknown.jdbcDriver", e.getMessage());
        }
    }

    public void testInit() throws IOException, SQLException
    {
        registry.register(EMBEDDED_DRIVER);

        restartDriverRegistry();
        
        assertTrue(registry.isRegistered(EMBEDDED_DRIVER));
    }

    public void testRegisterWithDriverManager() throws IOException
    {
        assertFalse(DriverManager.getDrivers().hasMoreElements());

        registry.register(EMBEDDED_DRIVER);

        assertTrue(DriverManager.getDrivers().hasMoreElements());
    }

    public void testUnregisterOnRestart() throws IOException, SQLException
    {
        registry.register(EMBEDDED_DRIVER);

        registry.unregisterOnRestart(EMBEDDED_DRIVER);

        restartDriverRegistry();

        assertFalse(registry.isRegistered(EMBEDDED_DRIVER));
        assertFalse(DriverManager.getDrivers().hasMoreElements());
    }

    private void restartDriverRegistry() throws SQLException
    {
        unregisterAllDriversFromDriverManager();

        registry = new DriverRegistry();
        registry.setDriverDir(driverDir);
        registry.init();
    }
}
