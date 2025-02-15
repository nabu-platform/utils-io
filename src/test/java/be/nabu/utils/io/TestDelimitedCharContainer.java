/*
* Copyright (C) 2014 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

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
	
	public void testBackedDelimitedWithRegexLineFeed() throws IOException {
		String string = "some\nfields";
		BackedDelimitedCharContainer delimited = new BackedDelimitedCharContainer(IOUtils.wrap(string), 12, "(\r\n|\n)", 2);
		assertEquals("some", IOUtils.toString(delimited));
		assertEquals("\n", delimited.getMatchedDelimiter());
		delimited = new BackedDelimitedCharContainer(IOUtils.wrap(delimited.getRemainder()), 12, "(\r\n|\n)", 2);
		assertEquals("fields", IOUtils.toString(delimited));
		assertNull(delimited.getMatchedDelimiter());
	}
	
	public void testBackedDelimitedWithRegexLineFeedAndCarriageReturn() throws IOException {
		String string = "some\r\nfields";
		BackedDelimitedCharContainer delimited = new BackedDelimitedCharContainer(IOUtils.wrap(string), 12, "(\r\n|\n)", 2);
		assertEquals("some", IOUtils.toString(delimited));
		assertEquals("\r\n", delimited.getMatchedDelimiter());
		delimited = new BackedDelimitedCharContainer(IOUtils.wrap(delimited.getRemainder()), 12, "(\r\n|\n)", 2);
		assertEquals("fields", IOUtils.toString(delimited));
		assertNull(delimited.getMatchedDelimiter());
	}
	
	public void testBackedDelimitedWithLineFeedMoreCombinations() throws IOException {
		String string = "some\r\nfields\rright\nhere";
		BackedDelimitedCharContainer delimited = new BackedDelimitedCharContainer(IOUtils.wrap(string), 30, "(\r\n|\n|\r)", 2);
		assertEquals("some", IOUtils.toString(delimited));
		assertEquals("\r\n", delimited.getMatchedDelimiter());
		delimited = new BackedDelimitedCharContainer(IOUtils.wrap(delimited.getRemainder()), 30, "(\r\n|\n|\r)", 2);
		assertEquals("fields", IOUtils.toString(delimited));
		assertEquals("\r", delimited.getMatchedDelimiter());
		delimited = new BackedDelimitedCharContainer(IOUtils.wrap(delimited.getRemainder()), 30, "(\r\n|\n|\r)", 2);
		assertEquals("right", IOUtils.toString(delimited));
		assertEquals("\n", delimited.getMatchedDelimiter());
		delimited = new BackedDelimitedCharContainer(IOUtils.wrap(delimited.getRemainder()), 30, "(\r\n|\n|\r)", 2);
		assertEquals("here", IOUtils.toString(delimited));
		assertNull(delimited.getMatchedDelimiter());
	}
	
	public void testDelimitedWithEscape() throws IOException {
		String string = "escaped quote\\\"here\"nothere";
		assertEquals("escaped quote\\\"here", (IOUtils.toString(IOUtils.delimit(IOUtils.wrap(string), "\"", '\\'))));
	}
}
