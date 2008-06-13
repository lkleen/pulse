package com.zutubi.prototype.type.record;

import com.zutubi.prototype.transaction.TransactionManager;
import com.zutubi.prototype.transaction.UserTransaction;
import com.zutubi.prototype.type.record.store.FileSystemRecordStore;
import com.zutubi.pulse.test.PulseTestCase;
import com.zutubi.pulse.util.FileSystemUtils;
import com.zutubi.util.Sort;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *
 *
 */
public class RecordManagerTest extends PulseTestCase
{
    private File tempDir;
    private RecordManager recordManager;
    private TransactionManager transactionManager;
    private UserTransaction userTransaction;

    protected void setUp() throws Exception
    {
        super.setUp();

        tempDir = FileSystemUtils.createTempDir(getName(), "");
        transactionManager = new TransactionManager();
        userTransaction = new UserTransaction(transactionManager);

        newRecordManager();
    }

    protected void tearDown() throws Exception
    {
        userTransaction = null;
        transactionManager = null;
        recordManager = null;
        if (!FileSystemUtils.rmdir(tempDir))
        {
            throw new RuntimeException("Unable to remove '" + tempDir + "' because your OS is not brown enough");
        }

        super.tearDown();
    }

    public void testInsert()
    {
        recordManager.insert("hello", new MutableRecordImpl());
        assertNotNull(recordManager.select("hello"));

        MutableRecordImpl record = new MutableRecordImpl();
        record.put("key", "value");
        recordManager.insert("hello/world", record);

        assertNotNull(recordManager.select("hello/world"));
        assertNotNull(recordManager.select("hello").get("world"));
    }

    public void testInsertAtEmptyString()
    {
        try
        {
            recordManager.insert("", new MutableRecordImpl());
            fail();
        }
        catch (IllegalArgumentException e)
        {
            // noop.
        }
    }

    public void testInsertNullRecord()
    {
        try
        {
            recordManager.insert("path", null);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            // noop.
        }
    }

    public void testInsertCreatesHandle()
    {
        Record record = new MutableRecordImpl();
        recordManager.insert("testpath", record);
        record = recordManager.select("testpath");
        assertEquals(1, record.getHandle());

        record = recordManager.select("testpath");
        assertEquals(1, record.getHandle());
    }

    public void testInsertNestedCreatesHandles()
    {
        MutableRecord record = new MutableRecordImpl();
        record.put("nested", new MutableRecordImpl());
        recordManager.insert("testpath", record);

        Record nested = recordManager.select("testpath/nested");
        assertEquals(2, nested.getHandle());
    }

    public void testInsertCreatesUniqueHandles()
    {
        recordManager.insert("r1", new MutableRecordImpl());
        recordManager.insert("r2", new MutableRecordImpl());
        Record r1 = recordManager.select("r1");
        Record r2 = recordManager.select("r2");
        assertNotSame(r1.getHandle(), r2.getHandle());
    }

    public void testHandlesAllocatedOnInsert()
    {
        // reinserting an existing record with a handle will result in a new record with a new handle.
        recordManager.insert("r1", new MutableRecordImpl());
        Record r1 = recordManager.select("r1");
        long h1 = r1.getHandle();
        recordManager.insert("r2", r1);
        Record r2 = recordManager.select("r2");
        assertEquals(h1, r1.getHandle());
        assertNotSame(r1.getHandle(), r2.getHandle());
    }

    public void testRecordArgumentNotChangedOnInsert()
    {
        MutableRecordImpl arg = new MutableRecordImpl();
        assertEquals(RecordManager.UNDEFINED, arg.getHandle());
        recordManager.insert("r1", arg);
        assertEquals(RecordManager.UNDEFINED, arg.getHandle());
    }

    public void testSelectByPath()
    {
        MutableRecordImpl record = new MutableRecordImpl();
        record.putMeta("key", "value");

        recordManager.insert("hello", new MutableRecordImpl());
        recordManager.insert("hello/world", record);
        assertNull(recordManager.select("hello/moon"));
        assertNull(recordManager.select("hello/world/key"));
        assertNotNull(recordManager.select("hello/world"));
    }

    public void testDelete()
    {
        MutableRecordImpl record = new MutableRecordImpl();
        record.putMeta("key", "value");

        recordManager.insert("hello", new MutableRecordImpl());
        recordManager.insert("hello/world", record);

        assertNull(recordManager.delete("hello/moon"));
        assertNull(recordManager.delete("hello/world/key"));
        assertNotNull(recordManager.delete("hello/world"));
        assertNull(recordManager.delete("hello/world"));
    }

    public void testMetaProperties()
    {
        MutableRecordImpl record = new MutableRecordImpl();
        record.putMeta("key", "value");
        recordManager.insert("path", record);

        Record loadedRecord = recordManager.select("path");
        assertEquals(record.getMeta("key"), loadedRecord.getMeta("key"));
    }

    public void testTrimmingPath()
    {
        MutableRecordImpl record = new MutableRecordImpl();
        record.putMeta("key", "value");

        recordManager.insert("another", record);

        assertNotNull(recordManager.select("/another"));
        assertNotNull(recordManager.select("another"));
        assertNotNull(recordManager.select("another/"));
    }

    public void testCopy()
    {
        MutableRecordImpl record = new MutableRecordImpl();
        record.put("key", "value");
        record.put("nested", record.copy(true));

        recordManager.insert("sourcePath", record);
        Record original = recordManager.select("sourcePath");
        assertNull(recordManager.select("destinationPath"));
        recordManager.copy("sourcePath", "destinationPath");

        Record copy = recordManager.select("destinationPath");
        assertNotNull(copy);
        assertEquals(original.get("key"), copy.get("key"));

        // ensure that the copies have unique handles.
        assertTrue(original.getHandle() != RecordManager.UNDEFINED);
        assertTrue(copy.getHandle() != RecordManager.UNDEFINED);
        assertTrue(original.getHandle() != copy.getHandle());
        assertTrue(((Record)original.get("nested")).getHandle() != ((Record)copy.get("nested")).getHandle());

        // ensure that changing the original does not change the copy
        record.put("anotherKey", "anotherValue");
        recordManager.update("sourcePath", original);

        copy = recordManager.select("destinationPath");
        assertFalse(copy.containsKey("anotherKey"));

    }

    public void testCopyOnInsert()
    {
        MutableRecordImpl original = new MutableRecordImpl();
        original.put("key", "value");

        recordManager.insert("path", original);

        // update the external record.
        original.put("key", "changedValue");

        Record storedRecord = recordManager.select("path");
        assertEquals("value", storedRecord.get("key"));
    }

    public void testContainsRecord()
    {
        assertFalse(recordManager.containsRecord("a"));
        recordManager.insert("a", new MutableRecordImpl());
        assertTrue(recordManager.containsRecord("a"));
    }

    public void testGetAllPaths()
    {
        recordManager.insert("a", new MutableRecordImpl());
        recordManager.insert("a/b", new MutableRecordImpl());
        recordManager.insert("a/b/c", new MutableRecordImpl());

        assertPaths("a", "a");
        assertPaths("a/b", "a/b");
        assertPaths("a/b/c", "a/b/c");
        assertPaths("a/b/c/d");

        assertPaths("a/*/c", "a/b/c");
        assertPaths("*/*/*", "a/b/c");

        recordManager.insert("a/b/d", new MutableRecordImpl());
        assertPaths("a/b/*", "a/b/c", "a/b/d");
    }

    public void testGetAllPathsMatchesSimpleKey()
    {
        MutableRecord record = new MutableRecordImpl();
        record.put("simple", "value");
        recordManager.insert("a", record);
        recordManager.insert("a/nested", new MutableRecordImpl());

        assertPaths("a/*", "a/nested");
    }

    private void assertPaths(String pattern, String... expected)
    {
        List<String> got = recordManager.getAllPaths(pattern);
        Collections.sort(got, new Sort.StringComparator());
        assertEquals(expected.length, got.size());
        for(int i = 0; i < expected.length; i++)
        {
            assertEquals(expected[i], got.get(i));
        }
    }

    public void testSelectAll()
    {
        // setup test data.
        recordManager.insert("a", new MutableRecordImpl());
        recordManager.insert("a/b", new MutableRecordImpl());
        recordManager.insert("a/b/c", new MutableRecordImpl());

        assertSelectedRecordCount("a", 1);
        assertSelectedRecordCount("a/b", 1);
        assertSelectedRecordCount("a/b/c", 1);
        assertSelectedRecordCount("a/b/c/d", 0);

        assertSelectedRecordCount("a/*/c", 1);
        assertSelectedRecordCount("*/*/*", 1);

        recordManager.insert("a/b/d", new MutableRecordImpl());
        assertSelectedRecordCount("a/b/*", 2);
    }

    public void testSelectAllHandlesNullInput()
    {
        try
        {
            recordManager.selectAll(null);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            // noop.
        }
    }

    public void testSelectAllMatchesSimpleKey()
    {
        MutableRecord record = new MutableRecordImpl();
        record.put("simple", "value");
        recordManager.insert("a", record);
        recordManager.insert("a/nested", new MutableRecordImpl());
        
        assertSelectedRecordCount("a/*", 1);
    }

    private void assertSelectedRecordCount(String path, int count)
    {
        Map<String, Record> records = recordManager.selectAll(path);
        assertEquals(count, records.size());
    }

    public void testSelectAllEmptyPath()
    {
        try
        {
            recordManager.selectAll("");
            fail();
        }
        catch (IllegalArgumentException e)
        {
            // noop.
        }
    }

    public void testSelectEmptyPath()
    {
        try
        {
            recordManager.select("");
            fail();
        }
        catch (IllegalArgumentException e)
        {
            // noop.
        }
    }

    public void testSelectHandlesNullInput()
    {
        try
        {
            recordManager.select(null);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            // noop.
        }
    }

    public void testHandlesAreUniqueAcrossRuns() throws Exception
    {
        long handle = 0;
        for (int i = 0; i < 10; i++)
        {
            newRecordManager();
            
            userTransaction.begin();
            for (int j = 0; j < i; j++)
            {
                String path = "i" + i + "j" + j;
                recordManager.insert(path, new MutableRecordImpl());
                long nextHandle = recordManager.select(path).getHandle();
                assertNextHandle(nextHandle, handle);
                handle = nextHandle;
            }
            userTransaction.commit();
        }
    }

    public void testCopyDoesNotDuplicateHandles()
    {
        recordManager.insert("r1", new MutableRecordImpl());
        recordManager.copy("r1", "r2");

        assertTrue(recordManager.select("r1").getHandle() != recordManager.select("r2").getHandle());
    }

    public void testHandleMap()
    {
        recordManager.insert("r1", new MutableRecordImpl());
        recordManager.insert("r2", new MutableRecordImpl());
        assertHandleToPath("r1");
        assertHandleToPath("r2");
    }

    public void testHandleMapAfterNestedInsert()
    {
        MutableRecord r = new MutableRecordImpl();
        r.put("nested", new MutableRecordImpl());
        recordManager.insert("testpath", r);
        assertHandleToPath("testpath");
        assertHandleToPath("testpath/nested");
    }

    public void testHandleMapNoSuchPath()
    {
        assertNull(recordManager.getPathForHandle(100));
    }

    public void testHandleMapAfterReload() throws Exception
    {
        recordManager.insert("r1", new MutableRecordImpl());
        recordManager.insert("r2", new MutableRecordImpl());
        assertHandleToPath("r1");
        assertHandleToPath("r2");

        newRecordManager();

        assertHandleToPath("r1");
        assertHandleToPath("r2");
    }

    public void testHandleMapAferDelete()
    {
        recordManager.insert("r1", new MutableRecordImpl());
        recordManager.insert("r2", new MutableRecordImpl());
        assertHandleToPath("r1");
        assertHandleToPath("r2");

        long handle =  recordManager.delete("r1").getHandle();
        assertNull(recordManager.getPathForHandle(handle));
        assertHandleToPath("r2");
    }

    public void testHandleMapAfterNestedDelete()
    {
        MutableRecord r = new MutableRecordImpl();
        r.put("nested", new MutableRecordImpl());
        recordManager.insert("testpath", r);

        Record record = recordManager.select("testpath");
        long outerHandle = record.getHandle();
        record = recordManager.select("testpath/nested");
        long innerHandle = record.getHandle();
        recordManager.delete("testpath");

        assertNull(recordManager.getPathForHandle(outerHandle));
        assertNull(recordManager.getPathForHandle(innerHandle));
    }

    public void testHandleMapAfterMove()
    {
        recordManager.insert("r1", new MutableRecordImpl());
        Record r = recordManager.select("r1");
        long handle = r.getHandle();
        recordManager.move("r1", "r2");
        assertEquals("r2", recordManager.getPathForHandle(handle));
    }
    
    private void assertHandleToPath(String path)
    {
        assertEquals(path, recordManager.getPathForHandle(recordManager.select(path).getHandle()));
    }

    private void assertNextHandle(long nextHandle, long handle)
    {
        assertTrue("Next handle '" + nextHandle + "' not higher than last '" + handle + "'", nextHandle > handle);
    }

    public void testMove()
    {
        MutableRecord record = new MutableRecordImpl();
        record.put("prop", "value");
        recordManager.insert("testpath", record);
        Record moved = recordManager.move("testpath", "newpath");

        // Check returned record has expected property
        assertEquals("value", moved.get("prop"));

        // Check we can load from move destination
        moved = recordManager.select("newpath");
        assertEquals("value", moved.get("prop"));

        // Check nothing at source
        assertNull(recordManager.select("testpath"));
    }

    public void testMovePreservesHandle()
    {
        Record record = new MutableRecordImpl();
        recordManager.insert("testpath", record);
        record = recordManager.select("testpath");
        long handle = record.getHandle();
        assertTrue(handle > 0);

        record = recordManager.move("testpath", "newpath");
        assertEquals(handle, record.getHandle());

        record = recordManager.select("newpath");
        assertEquals(handle, record.getHandle());
    }

    public void testMoveIsDeep()
    {
        MutableRecord record = new MutableRecordImpl();
        MutableRecord nested = new MutableRecordImpl();
        record.put("prop", "val");
        record.put("nested", nested);
        nested.put("prop", "nestedval");

        recordManager.insert("testpath", record);
        Record inserted = recordManager.select("testpath");
        Record insertedNest = (Record) inserted.get("nested");
        assertTrue(insertedNest.getHandle() > 0);

        Record moved = recordManager.move("testpath", "newpath");
        assertEquals("val", moved.get("prop"));
        Record movedNest = (Record) moved.get("nested");
        assertEquals(insertedNest.getHandle(), movedNest.getHandle());
        assertEquals("nestedval", movedNest.get("prop"));

        Record loadedNest = recordManager.select("newpath/nested");
        assertEquals(insertedNest.getHandle(), loadedNest.getHandle());
        assertEquals("nestedval", loadedNest.get("prop"));

        assertNull(recordManager.select("testpath/nested"));
    }

    public void testUpdateRemovesKeys()
    {
        MutableRecord record = new MutableRecordImpl();
        record.put("foo", "bar");

        recordManager.insert("path", record);

        Record loaded = recordManager.select("path");

        record = new MutableRecordImpl();
        record.setHandle(loaded.getHandle());

        recordManager.update("path", record);

        loaded = recordManager.select("path");
        assertEquals(0, loaded.size());
    }

    public void testUpdateDoesNotRemoveNested()
    {
        MutableRecord record = new MutableRecordImpl();
        record.put("foo", "bar");
        record.put("quux", new MutableRecordImpl());

        recordManager.insert("path", record);

        Record loaded = recordManager.select("path");

        record = new MutableRecordImpl();
        record.setHandle(loaded.getHandle());

        recordManager.update("path", record);

        loaded = recordManager.select("path");
        assertEquals(1, loaded.size());
        assertNotNull(loaded.get("quux"));
    }

    public void testUpdateExistingPathUsingExistingRecordFails()
    {
        MutableRecord x = new MutableRecordImpl();
        x.put("foo", "bar");

        MutableRecord y = new MutableRecordImpl();
        y.put("a", "b");

        recordManager.insert("x", x);
        recordManager.insert("y", y);
        try
        {
            recordManager.update("x", y);
            fail();
        }
        catch (RuntimeException e)
        {
            assertEquals("Failed to update 'x'. New handle differs from existing handle.", e.getMessage());
        }
    }

    //---( transactional tests: focus on record handles )---

    public void testCommit()
    {
        userTransaction.begin();

        MutableRecord record = new MutableRecordImpl();
        record.put("foo", "bar");

        recordManager.insert("path", record);
        final Record insertedRecord = recordManager.select("path");

        assertNotNull(recordManager.select("path"));
        executeOnSeparateThreadAndWait(new Runnable()
        {
            public void run()
            {
                assertNull(recordManager.select("path"));
                assertNull(recordManager.getPathForHandle(insertedRecord.getHandle()));
            }
        });

        userTransaction.commit();

        assertNotNull(recordManager.select("path"));
        executeOnSeparateThreadAndWait(new Runnable()
        {
            public void run()
            {
                assertNotNull(recordManager.select("path"));
                assertNotNull(recordManager.getPathForHandle(insertedRecord.getHandle()));
            }
        });
    }

    public void testRollback()
    {
        userTransaction.begin();

        MutableRecord record = new MutableRecordImpl();
        record.put("foo", "bar");

        recordManager.insert("path", record);

        assertNotNull(recordManager.select("path"));
        executeOnSeparateThreadAndWait(new Runnable()
        {
            public void run()
            {
                assertNull(recordManager.select("path"));
            }
        });

        final Record insertedRecord = recordManager.select("path");

        userTransaction.rollback();

        assertNull(recordManager.select("path"));
        assertNull(recordManager.getPathForHandle(insertedRecord.getHandle()));
    }

    public void testNoSurroundingTransaction()
    {
        MutableRecord record = new MutableRecordImpl();
        record.put("foo", "bar");

        recordManager.insert("path", record);
        final Record insertedRecord = recordManager.select("path");

        assertNotNull(recordManager.select("path"));
        executeOnSeparateThreadAndWait(new Runnable()
        {
            public void run()
            {
                assertNotNull(recordManager.select("path"));
            }
        });

        assertEquals("path", recordManager.getPathForHandle(insertedRecord.getHandle()));
        executeOnSeparateThreadAndWait(new Runnable()
        {
            public void run()
            {
                assertEquals("path", recordManager.getPathForHandle(insertedRecord.getHandle()));
            }
        });
    }

    private void newRecordManager() throws Exception
    {
        FileSystemRecordStore recordStore = new FileSystemRecordStore();
        recordStore.setTransactionManager(transactionManager);
        recordStore.setPersistenceDirectory(tempDir);
        recordStore.init();

        recordManager = new RecordManager();
        recordManager.setTransactionManager(transactionManager);
        recordManager.setRecordStore(recordStore);
        recordManager.init();

    }
}
