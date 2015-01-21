package be.nabu.utils.io;

import java.io.IOException;

import junit.framework.TestCase;
import be.nabu.utils.io.api.CharBuffer;
import be.nabu.utils.io.api.DelimitedCharContainer;
import be.nabu.utils.io.api.MarkableContainer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.containers.LimitedMarkableContainer;
import be.nabu.utils.io.containers.chars.BackedDelimitedCharContainer;

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

	public void testBackedDelimited() throws IOException {
		String string = "some,fields,*here";
		MarkableContainer<CharBuffer> original = IOUtils.mark(IOUtils.wrap(string));
		original.mark();
		
		// try one delimiter with too big a buffer size
		BackedDelimitedCharContainer delimited = new BackedDelimitedCharContainer(original, 6, ",");
		assertEquals(string.substring(0, 4), IOUtils.toString(delimited));
		assertEquals(",", delimited.getMatchedDelimiter());
		assertEquals("f", delimited.getRemainder());
		original.reset();
		IOUtils.skipChars(original, 6 - delimited.getRemainder().length());
		
		delimited = new BackedDelimitedCharContainer(original, 10, ",*");
		assertEquals("fields", IOUtils.toString(delimited));
		assertEquals(",*", delimited.getMatchedDelimiter());
		assertEquals("he", delimited.getRemainder());
	}
	
	public void testBackedDelimitedWithLongSeparator() throws IOException {
		String string = "some,fields,*here";
		MarkableContainer<CharBuffer> original = IOUtils.mark(IOUtils.wrap(string));
		original.mark();
		// get a delimited but with a buffer size that is smack in the middle of the delimiter
		BackedDelimitedCharContainer delimited = new BackedDelimitedCharContainer(original, 12, ",*");
		assertEquals("some,fields", IOUtils.toString(delimited));
	}
	
	public void testBackedDelimitedWithRegex() throws IOException {
		String string = "some::fields;;here:;";
		LimitedMarkableContainer<CharBuffer> original = new LimitedMarkableContainer<CharBuffer>(IOUtils.wrap(string), 0);
		original.mark();
		BackedDelimitedCharContainer delimited = new BackedDelimitedCharContainer(original, 12, "[:;]{2}", 2);
		assertEquals("some", IOUtils.toString(delimited));
		assertEquals("::", delimited.getMatchedDelimiter());
		
		original.moveMarkRelative(6);
		original.reset();
		delimited = new BackedDelimitedCharContainer(original, 12, "[:;]{2}", 2);
		assertEquals("fields", IOUtils.toString(delimited));
		assertEquals(";;", delimited.getMatchedDelimiter());
		
		original.moveMarkRelative(8);
		original.reset();
		delimited = new BackedDelimitedCharContainer(original, 12, "[:;]{2}", 2);
		assertEquals("here", IOUtils.toString(delimited));
		assertEquals(":;", delimited.getMatchedDelimiter());
	}
	
	
	public void testBackedDelimitedWithRegexNoEnd() throws IOException {
		String string = "some::fields";
		LimitedMarkableContainer<CharBuffer> original = new LimitedMarkableContainer<CharBuffer>(IOUtils.wrap(string), 0);
		original.mark();
		BackedDelimitedCharContainer delimited = new BackedDelimitedCharContainer(original, 12, "[:;]{2}", 2);
		assertEquals("some", IOUtils.toString(delimited));
		assertEquals("::", delimited.getMatchedDelimiter());

		original.moveMarkRelative(6);
		original.reset();
		delimited = new BackedDelimitedCharContainer(original, 12, "[:;]{2}", 2);
		assertEquals("fields", IOUtils.toString(delimited));
		assertNull(delimited.getMatchedDelimiter());
	}
}
