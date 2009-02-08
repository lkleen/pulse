package com.zutubi.pulse.util;

import com.zutubi.pulse.test.PulseTestCase;

import java.util.Arrays;
import java.util.List;

/**
 */
public class StringUtilsTest extends PulseTestCase
{
    public void testTrimStringShort()
    {
        assertEquals("12345", StringUtils.trimmedString("12345", 10));
    }

    public void testTrimStringMuchLonger()
    {
        assertEquals("12...", StringUtils.trimmedString("1234567890", 5));
    }

    public void testTrimStringExact()
    {
        assertEquals("12345", StringUtils.trimmedString("12345", 5));
    }

    public void testTrimStringJustOver()
    {
        assertEquals("12...", StringUtils.trimmedString("123456", 5));
    }

    public void testTrimStringShortLimit()
    {
        assertEquals("..", StringUtils.trimmedString("12345", 2));
    }

    public void testTrimStringDotsLimit()
    {
        assertEquals("...", StringUtils.trimmedString("12345", 3));
    }

    public void testTrimStringZeroLimit()
    {
        assertEquals("", StringUtils.trimmedString("12345", 0));
    }

    public void testTrimStringNegativeLimit()
    {
        try
        {
            StringUtils.trimmedString("", -1);
            fail();
        }
        catch(IllegalArgumentException e)
        {
        }
    }

    public void testTrimStringCustomMessage()
    {
        assertEquals("this is a long s... [my message]", StringUtils.trimmedString("this is a long string that will need trimming", 32, "... [my message]"));
    }

    public void testTrimStringCustomMessageTooLong()
    {
        assertEquals("... [my me", StringUtils.trimmedString("this is a long string that will need trimming", 10, "... [my message]"));
    }

    public void testWrapShort()
    {
        assertEquals("12345", StringUtils.wrapString("12345", 10, null));
    }

    public void testWrapSimple()
    {
        assertEquals("12345\n67890", StringUtils.wrapString("12345 67890", 5, null));
    }

    public void testWrapEarlierSpace()
    {
        assertEquals("123\n4567", StringUtils.wrapString("123 4567", 5, null));
    }

    public void testWrapMultiline()
    {
        assertEquals("this is a\nvery fine\nmultiline\nexample", StringUtils.wrapString("this is a very fine multiline example", 9, null));
    }

    public void testWrapNoSpace()
    {
        assertEquals("12345\n67890", StringUtils.wrapString("1234567890", 5, null));
    }

    public void testWrapPrefix()
    {
        assertEquals("12345\n=6789\n=0", StringUtils.wrapString("1234567890", 5, "="));
    }

    public void testWrapSomeText()
    {
        assertEquals("  * this is a sample of the\n" +
                "    sorts of wacky things that\n" +
                "    we might need the wrapping\n" +
                "    function to have a go at,\n" +
                "    including the possibility\n" +
                "    of long\n" +
                "    striiiiiiiiiiiiiiiiiiiiiii\n" +
                "    iiiiiiiiiiiiiiiiiiiiiiiiii\n" +
                "    iiiiiiiiiiiiiiiiiiiiiiiiii\n" +
                "    iiiiiiiiiiiiiiiiiiiiiiiiii\n" +
                "    iiiiiiiiiiiiiiiiiiiiiiiiii\n" +
                "    iiiiiiiiiiiiiiiiiiiiiiiiii\n" +
                "    iiiiiiiiiiiiiiiiiiiiiiiiin\n" +
                "    gs of random junk to throw\n" +
                "    things right out of wack",
                StringUtils.wrapString("  * this is a sample of the sorts of wacky things " +
                        "that we might need the wrapping function to have a go " +
                        "at, including the possibility of long striiiiiiiiiiiiii" +
                        "iiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiii" +
                        "iiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiii" +
                        "iiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiin" +
                        "gs of random junk to throw things right out of wack",
                        30, "    "));
    }

    public void testInvalidPrefix()
    {
        try
        {
            StringUtils.wrapString("", 3, "pr");
            fail();
        }
        catch (IllegalArgumentException e)
        {

        }
    }

    public void testWrapLineOnSpaceOnly()
    {
        assertEquals("1234567890", StringUtils.wrapString("1234567890", 5, null, false));
        assertEquals("12345678\n90", StringUtils.wrapString("12345678\n90", 5, null, false));
        assertEquals("12345678\n 90", StringUtils.wrapString("12345678 90", 5, null, false));
    }

    public void testGetLineLinefeed()
    {
        assertEquals("string", StringUtils.getLine("some\nstring\nhere", 2));
    }

    public void testGetLineCarriageReturn()
    {
        assertEquals("string", StringUtils.getLine("some\rstring\rhere", 2));
    }

    public void testGetLineCarriageReturnLinefeed()
    {
        assertEquals("string", StringUtils.getLine("some\r\nstring\r\nhere", 2));
    }

    public void testGetLineMixed()
    {
        assertEquals("w00t", StringUtils.getLine("using\r\ndifferent\nsplitters\rw00t\r\nto\nconfuse\r\nthings\n", 4));
    }

    public void testGetLineEmpty()
    {
        assertEquals("", StringUtils.getLine("third\nline\n\nis empty", 3));
    }

    public void testGetLinePastEnd()
    {
        assertNull(StringUtils.getLine("some\nlines\nhere", 4));
    }

    public void testSplitEmpty()
    {
        splitHelper("");
    }

    public void testUnsplitEmpty()
    {
        unsplitHelper("");
    }

    public void testSplitSpace()
    {
        splitHelper(" ");
    }

    public void testSplitSpaces()
    {
        splitHelper("   ");
    }

    public void testSplitSimple()
    {
        splitHelper("one two", "one", "two");
    }

    public void testUnsplitSimple()
    {
        unsplitHelper("one two", "one", "two");
    }

    public void testSplitMore()
    {
        splitHelper("one two  three   four", "one", "two", "three", "four");
    }

    public void testUnsplitMore()
    {
        unsplitHelper("one two three four", "one", "two", "three", "four");
    }

    public void testSplitEscape()
    {
        splitHelper("one\\ two", "one two");
    }

    public void testSplitEscapeBackslash()
    {
        splitHelper("one\\\\ two", "one\\", "two");
    }

    public void testUnsplitEscapeBackslash()
    {
        unsplitHelper("one\\\\ two", "one\\", "two");
    }

    public void testSplitQuotes()
    {
        splitHelper("hello \"you idiot\" there", "hello", "you idiot", "there");
    }

    public void testUnsplitQuotes()
    {
        unsplitHelper("hello \"you idiot\" there", "hello", "you idiot", "there");
    }

    public void testSplitEscapeQuote()
    {
        splitHelper("one\\\" two", "one\"", "two");
    }

    public void testUnsplitEscapeQuote()
    {
        unsplitHelper("one\\\" two", "one\"", "two");
    }

    public void testSplitEscapeQuoteInQuotes()
    {
        splitHelper("\"one\\\"two\"", "one\"two");
    }

    public void testSplitQuoteInMiddle()
    {
        splitHelper("word\"quoted words\"another word", "wordquoted wordsanother", "word");
    }

    public void testSplitEmptyQuotesInMiddle()
    {
        splitHelper("word\"\"another word", "wordanother", "word");
    }

    public void testSplitQuoteEmpty()
    {
        splitHelper("\"\"", "");
    }

    public void testUnsplitQuoteEmpty()
    {
        unsplitHelper("\"\"", "");
    }

    public void testSplitQuoteEmptyAmongst()
    {
        splitHelper("wow \"\" empty", "wow", "", "empty");
    }

    public void testUnsplitQuoteEmptyAmongst()
    {
        unsplitHelper("wow \"\" empty", "wow", "", "empty");
    }

    public void testEndsInBackslash()
    {
        try
        {
            StringUtils.split("bad ending \\");
            fail();
        }
        catch(IllegalArgumentException e)
        {
            assertEquals("Unexpected end of input after backslash (\\)", e.getMessage());
        }
    }

    public void testUnfinishedQuotes()
    {
        try
        {
            StringUtils.split("\"bad ending");
            fail();
        }
        catch(IllegalArgumentException e)
        {
            assertEquals("Unexpected end of input looking for end of quote (\")", e.getMessage());
        }
    }

    public void testStringEquals()
    {
        assertTrue(StringUtils.equals(null, null));
        assertTrue(StringUtils.equals("", ""));
        assertTrue(StringUtils.equals("a", "a"));
        assertTrue(StringUtils.equals("abcde", "abcde"));

        assertFalse(StringUtils.equals("", null));
        assertFalse(StringUtils.equals("vvv", null));
        assertFalse(StringUtils.equals(null, ""));
        assertFalse(StringUtils.equals(null, "aa"));
        assertFalse(StringUtils.equals("aaa", "bbb"));
    }

    public void testJoin()
    {
        assertEquals("a,b,c", StringUtils.join(",", "a", "b", "c"));
    }

    public void testJoinGlueAllOver()
    {
        assertEquals(",a,,,b,,,c,", StringUtils.join(",", false, ",a,", ",b,", ",c,"));
    }

    public void testJoinGlueCheckNothingToDo()
    {
        assertEquals("a,b,c", StringUtils.join(",", true, "a", "b", "c"));
    }

    public void testJoinGlueCheckGlueAllOver()
    {
        assertEquals(",a,b,c,", StringUtils.join(",", true, ",a,", ",b,", ",c,"));
    }

    public void testJoinEmptyGlue()
    {
        assertEquals("abc", StringUtils.join("", true, "a", "b", "c"));    
    }

    public void testJoinEmptyPieces()
    {
        assertEquals(",,a,,b,,,c,", StringUtils.join(",", true, "", "", "a", "", "b", "", "", "c", ""));
    }

    public void testJoinGlueCheckGluePieces()
    {
        assertEquals(",a,,b,", StringUtils.join(",", true, ",", "a", ",", "b", ","));        
    }

    public void testJoinSkipEmpty()
    {
        assertEquals("a,b,c", StringUtils.join(",", true, true, "", "", "a", "", "b", "", "", "c", ""));
    }

    public void testUrlEncodePath()
    {
        assertEquals("foo%20bar/baz+quux%3Fquuux", StringUtils.urlEncodePath("foo bar/baz+quux?quuux"));
    }
    
    public void testUrlEncodePathAbsolute()
    {
        assertEquals("/absolute%20path", StringUtils.urlEncodePath("/absolute path"));
    }

    public void testUrlEncodePathDoubleSlash()
    {
        assertEquals("//absolute/path+here", StringUtils.urlEncodePath("//absolute/path+here"));
    }

    public void testUrlEncodePathDoubleSlashMiddle()
    {
        assertEquals("double//slash/in+the%20path", StringUtils.urlEncodePath("double//slash/in+the path"));
    }

    public void testUrlEncodePathEmpty()
    {
        assertEquals("", StringUtils.urlEncodePath(""));
    }

    public void testTimesEmptyString()
    {
        assertEquals("", StringUtils.times("", 10));
    }

    public void testTimesZeroCount()
    {
        assertEquals("", StringUtils.times("hello", 0));
    }
    
    public void testTimesOneCount()
    {
        assertEquals("hello", StringUtils.times("hello", 1));
    }

    public void testTimes()
    {
        assertEquals("123123123", StringUtils.times("123", 3));
    }

    private void splitHelper(String s, String... expected)
    {
        List<String> expectedParts = Arrays.asList(expected);
        List<String> gotParts = StringUtils.split(s);
        assertEquals(expectedParts, gotParts);
    }

    private void unsplitHelper(String expected, String... pieces)
    {
        String got = StringUtils.unsplit(Arrays.asList(pieces));
        assertEquals(expected, got);
    }
}