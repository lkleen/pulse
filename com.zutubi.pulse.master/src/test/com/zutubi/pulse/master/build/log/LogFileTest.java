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

package com.zutubi.pulse.master.build.log;

import com.zutubi.pulse.core.test.api.PulseTestCase;
import com.zutubi.util.StringUtils;
import com.zutubi.util.io.FileSystemUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.List;

import static java.util.Collections.nCopies;

public class LogFileTest extends PulseTestCase
{
    private static final int COMPRESS_THRESHOLD = 1024;
    private static final int TAIL_LIMIT = 10;

    private static final String TEST_CONTENT = "hello\nworld\n";
    private static final String TEST_LINE = "some line\n";

    private File tempDir;
    private File file;
    private LogFile logFile;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        tempDir = FileSystemUtils.createTempDir(getName(), ".tmp");
        file = new File(tempDir, "test.log");
        logFile = new LogFile(file, COMPRESS_THRESHOLD, TAIL_LIMIT, true);
    }

    @Override
    protected void tearDown() throws Exception
    {
        removeDirectory(tempDir);
        super.tearDown();
    }

    public void testSimpleWrite() throws IOException
    {
        writeLines(TEST_CONTENT);
        assertFalse(logFile.isCompressed());

        assertEquals(TEST_CONTENT, getContents());
    }

    public void testMultipleWrites() throws IOException
    {
        writeLines(TEST_CONTENT);
        assertFalse(logFile.isCompressed());
        writeLines(TEST_CONTENT);
        assertFalse(logFile.isCompressed());

        assertEquals(TEST_CONTENT + TEST_CONTENT, getContents());
    }

    public void testCompressed() throws IOException
    {
        String written = writeCompressed();
        assertTrue(logFile.isCompressed());
        assertEquals(written, getContents());
    }

    public void testCompressionDisabled() throws IOException
    {
        logFile = new LogFile(file, COMPRESS_THRESHOLD, TAIL_LIMIT, false);
        String written = writeCompressed();
        assertFalse(logFile.isCompressed());
        assertEquals(written, getContents());
    }

    public void testMultipleWritesCompressed() throws IOException
    {
        String written = writeCompressed();
        Thread.yield();
        assertTrue(logFile.isCompressed());
        writeLines(TEST_CONTENT);
        assertTrue(logFile.isCompressed());
        assertEquals(written + TEST_CONTENT, getContents());
    }
    
    public void testTail() throws IOException
    {
        final String LINE_1 = "line 1\n";
        final String LINE_2 = "line 2\n";
        final String LINE_3 = "line 3\n";
        final String LINE_4 = "line 4\n";

        writeLines(LINE_1, LINE_2, LINE_3, LINE_4);

        assertEquals(StringUtils.join("", LINE_3, LINE_4), logFile.getTail(2));
        String allLines = StringUtils.join("", LINE_1, LINE_2, LINE_3, LINE_4);
        assertEquals(allLines, logFile.getTail(4));
        assertEquals(allLines, logFile.getTail(5));
        assertEquals(allLines, logFile.getTail(TAIL_LIMIT));
        assertEquals(allLines, logFile.getTail(TAIL_LIMIT + 2));
    }

    public void testTailCompressed() throws IOException
    {
        final String LINE_4 = "line -4\n";
        final String LINE_3 = "line -3\n";
        final String LINE_2 = "line -2\n";
        final String LINE_1 = "line -1\n";

        String fourLines = StringUtils.join("", LINE_4, LINE_3, LINE_2, LINE_1);

        writeCompressed();
        writeLines(LINE_4, LINE_3, LINE_2, LINE_1);

        assertEquals(StringUtils.join("", LINE_2, LINE_1), logFile.getTail(2));
        assertEquals(fourLines, logFile.getTail(4));
        assertEquals(TEST_LINE + fourLines, logFile.getTail(5));
        assertEquals(nTestLines(TAIL_LIMIT - 4) + fourLines, logFile.getTail(TAIL_LIMIT));
        assertEquals(nTestLines(TAIL_LIMIT - 2) + fourLines, logFile.getTail(TAIL_LIMIT + 2));
    }
     
    public void testNotCompressedWhileReaderOpen() throws IOException
    {
        writeLines(TEST_CONTENT);
        assertFalse(logFile.isCompressed());
        LogFile other = new LogFile(file, COMPRESS_THRESHOLD, TAIL_LIMIT, true);
        InputStream in = other.openStream();

        writeCompressed();
        assertFalse(logFile.isCompressed());

        in.close();
        assertTrue(logFile.isCompressed());
    }

    public void testOpenSecondWriterOnCompressedWhileReaderOpen() throws IOException
    {
        String compressedContent = writeCompressed();
        assertTrue(logFile.isCompressed());

        Writer writer = logFile.openWriter();
        assertFalse(logFile.isCompressed());

        assertEquals(compressedContent, logFile.asCharSource(Charset.defaultCharset()).read());

        assertFalse(logFile.isCompressed());

        writer.write(TEST_CONTENT);
        writer.flush();
        assertFalse(logFile.isCompressed());
        
        // A reader open now should see the fresh content, despite a stale
        // gzip file.
        assertEquals(compressedContent + TEST_CONTENT, getContents());

        writer.close();
        assertTrue(logFile.isCompressed());

        assertEquals(compressedContent + TEST_CONTENT, getContents());
    }

    private String nTestLines(int n)
    {
        return StringUtils.join("", nCopies(n, TEST_LINE));
    }

    private String writeCompressed() throws IOException
    {
        List<String> lines = nCopies(COMPRESS_THRESHOLD / TEST_LINE.length() + 1, TEST_LINE);
        writeLines(lines.toArray(new String[lines.size()]));
        return StringUtils.join("", lines);
    }

    private void writeLines(String... lines) throws IOException
    {
        Writer writer = logFile.openWriter();
        for (String line: lines)
        {
            writer.write(line);
        }
        writer.close();
    }

    private String getContents() throws IOException
    {
        String content = logFile.asCharSource(Charset.defaultCharset()).read();
        return content.replace("\r\n", "\n");
    }

}