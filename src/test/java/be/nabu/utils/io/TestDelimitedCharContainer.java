package be.nabu.utils.io;

import java.io.IOException;

import junit.framework.TestCase;
import be.nabu.utils.io.api.CharBuffer;
import be.nabu.utils.io.api.DelimitedCharContainer;
import be.nabu.utils.io.api.MarkableContainer;
import be.nabu.utils.io.api.ReadableContainer;

public class TestDelimitedCharContainer extends TestCase {
	
	public void testNonMatch() throws IOException {
		ReadableContainer<CharBuffer> original = IOUtils.wrap("some,fields,here");
		assertEquals("some,fields,here", IOUtils.toString(IOUtils.delimit(original, ";")));
	}
	
	public void testFixedDelimited() throws IOException {
		ReadableContainer<CharBuffer> original = IOUtils.wrap("some,fields,here");
		assertEquals("some", IOUtils.toString(IOUtils.delimit(original, ",")));
		assertEquals("fields", IOUtils.toString(IOUtils.delimit(original, ",")));
		assertEquals("here", IOUtils.toString(IOUtils.delimit(original, ",")));
		DelimitedCharContainer container = IOUtils.delimit(original, ",");
		assertTrue(IOUtils.toString(container).isEmpty());
		assertFalse(container.isDelimiterFound());
	}

	public void testRegexDelimited() throws IOException {
		ReadableContainer<CharBuffer> original = IOUtils.wrap("some;variable$-$delimiters,here");
		String regex = ".*(;|[$-]{3}|,).*";
		assertEquals("some", IOUtils.toString(IOUtils.delimit(original, regex, 3)));
		assertEquals("variable", IOUtils.toString(IOUtils.delimit(original, regex, 3)));
		assertEquals("delimiters", IOUtils.toString(IOUtils.delimit(original, regex, 3)));
		assertEquals("here", IOUtils.toString(IOUtils.delimit(original, regex, 3)));
		DelimitedCharContainer container = IOUtils.delimit(original, regex, 3);
		assertTrue(IOUtils.toString(container).isEmpty());
		assertFalse(container.isDelimiterFound());
	}

	/**
	 * The delimited stream built after the last "," will not read anything, hence return -1
	 * Note that you need to use the delimiterFound() on the last one to check if it actually ended in a delimiter or because the data was done
	 */
	public void testFixedDelimitedEnd() throws IOException {
		ReadableContainer<CharBuffer> original = IOUtils.wrap("some,fields,here,");
		assertEquals("some", IOUtils.toString(IOUtils.delimit(original, ",")));
		assertEquals("fields", IOUtils.toString(IOUtils.delimit(original, ",")));
		assertEquals("here", IOUtils.toString(IOUtils.delimit(original, ",")));
		assertTrue(IOUtils.toString(IOUtils.delimit(original, ",")).isEmpty());
		DelimitedCharContainer container = IOUtils.delimit(original, ",");
		assertTrue(IOUtils.toString(container).isEmpty());
		assertFalse(container.isDelimiterFound());
	}

	public void testRegexDelimitedEnd() throws IOException {
		ReadableContainer<CharBuffer> original = IOUtils.wrap("some;variable$-$delimiters,here;,");
		String regex = ".*(;|[$-]{3}|,).*";
		assertEquals("some", IOUtils.toString(IOUtils.delimit(original, regex, 3)));
		assertEquals("variable", IOUtils.toString(IOUtils.delimit(original, regex, 3)));
		assertEquals("delimiters", IOUtils.toString(IOUtils.delimit(original, regex, 3)));
		assertEquals("here", IOUtils.toString(IOUtils.delimit(original, regex, 3)));
		assertTrue(IOUtils.toString(IOUtils.delimit(original, regex, 3)).isEmpty());
		assertTrue(IOUtils.toString(IOUtils.delimit(original, regex, 3)).isEmpty());
	}
	
	public void testLimitedDelimiterSearch() throws IOException {
		String string = "some,fields,here";
		
		// we set a limit on how far we want to search for a delimiter
		int limit = 8;
		MarkableContainer<CharBuffer> original = IOUtils.mark(IOUtils.wrap(string));
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
		
		// reset again just to test resets
		original.reset();
		delimited = IOUtils.delimit(IOUtils.limitReadable(original, limit), ",");
		assertEquals("some", IOUtils.toString(delimited));
		assertTrue(delimited.isDelimiterFound());

		
		assertEquals("fields,here", IOUtils.toString(original));

	}

}
