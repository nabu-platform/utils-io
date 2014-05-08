package be.nabu.utils.io;

import java.io.IOException;

import be.nabu.utils.io.api.DelimitedCharContainer;
import be.nabu.utils.io.api.MarkableCharContainer;
import be.nabu.utils.io.api.ReadableCharContainer;
import junit.framework.TestCase;

public class TestDelimitedCharContainer extends TestCase {
	
	public void testNonMatch() {
		ReadableCharContainer original = IOUtils.wrap("some,fields,here");
		assertEquals("some,fields,here", IOUtils.toString(IOUtils.delimit(original, ";")));
	}
	
	public void testFixedDelimited() {
		ReadableCharContainer original = IOUtils.wrap("some,fields,here");
		assertEquals("some", IOUtils.toString(IOUtils.delimit(original, ",")));
		assertEquals("fields", IOUtils.toString(IOUtils.delimit(original, ",")));
		assertEquals("here", IOUtils.toString(IOUtils.delimit(original, ",")));
		assertNull(IOUtils.toString(IOUtils.delimit(original, ",")));
	}
	
	public void testRegexDelimited() {
		ReadableCharContainer original = IOUtils.wrap("some;variable$-$delimiters,here");
		String regex = ".*(;|[$-]{3}|,).*";
		assertEquals("some", IOUtils.toString(IOUtils.delimit(original, regex, 3)));
		assertEquals("variable", IOUtils.toString(IOUtils.delimit(original, regex, 3)));
		assertEquals("delimiters", IOUtils.toString(IOUtils.delimit(original, regex, 3)));
		assertEquals("here", IOUtils.toString(IOUtils.delimit(original, regex, 3)));
		assertNull(IOUtils.toString(IOUtils.delimit(original, regex, 3)));
	}

	/**
	 * The delimited stream built after the last "," will not read anything, hence return -1 which result in a null in the toString()
	 * Note that you need to use the delimiterFound() on the last one to check if it actually ended in a delimiter or because the data was done
	 */
	public void testFixedDelimitedEnd() throws IOException {
		ReadableCharContainer original = IOUtils.wrap("some,fields,here,");
		assertEquals("some", IOUtils.toString(IOUtils.delimit(original, ",")));
		assertEquals("fields", IOUtils.toString(IOUtils.delimit(original, ",")));
		assertEquals("here", IOUtils.toString(IOUtils.delimit(original, ",")));
		assertNull(IOUtils.toString(IOUtils.delimit(original, ",")));
		assertNull(IOUtils.toString(IOUtils.delimit(original, ",")));
	}

	public void testRegexDelimitedEnd() {
		ReadableCharContainer original = IOUtils.wrap("some;variable$-$delimiters,here;,");
		String regex = ".*(;|[$-]{3}|,).*";
		assertEquals("some", IOUtils.toString(IOUtils.delimit(original, regex, 3)));
		assertEquals("variable", IOUtils.toString(IOUtils.delimit(original, regex, 3)));
		assertEquals("delimiters", IOUtils.toString(IOUtils.delimit(original, regex, 3)));
		assertEquals("here", IOUtils.toString(IOUtils.delimit(original, regex, 3)));
		assertTrue(IOUtils.toString(IOUtils.delimit(original, regex, 3)).isEmpty());
		assertNull(IOUtils.toString(IOUtils.delimit(original, regex, 3)));
	}
	
	public void testLimitedDelimiterSearch() {
		String string = "some,fields,here";
		
		// we set a limit on how far we want to search for a delimiter
		int limit = 8;
		MarkableCharContainer original = IOUtils.wrapMarkable(IOUtils.wrap(string));
		original.mark();

		// try one delimiter
		DelimitedCharContainer delimited = IOUtils.delimit(IOUtils.limitReadable(original, limit), ";");
		assertEquals(string.substring(0, limit), IOUtils.toString(delimited));
		assertFalse(delimited.isDelimiterFound());
		
		// reset to try another
		original.reset();
		delimited = IOUtils.delimit(IOUtils.limitReadable(original, limit), ",");
		assertEquals("some", IOUtils.toString(delimited));
		assertTrue(delimited.isDelimiterFound());
		assertEquals("fields,here", IOUtils.toString(original));
	}

}
