package be.nabu.utils.io;

import java.io.IOException;
import java.nio.charset.Charset;

import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.CharBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import junit.framework.TestCase;

public class TestUnwrap extends TestCase {
	
	public void testUnwrap() throws IOException {
		ReadableContainer<CharBuffer> chars = IOUtils.wrap("t�st");
		ReadableContainer<ByteBuffer> bytes = IOUtils.unwrap(chars, Charset.forName("UTF-8"));
		byte [] array = IOUtils.toBytes(bytes);
		assertEquals(5, array.length);
		assertEquals(116, (int) array[0] & 0xff);
		// c3 a9 = é in UTF-8
		assertEquals(0xc3, (int) array[1] & 0xff);
		assertEquals(0xa9, (int) array[2] & 0xff);
		assertEquals(115, (int) array[3] & 0xff);
		assertEquals(116, (int) array[4] & 0xff);
	}
	
	public void testComplexUnwrap() throws IOException {
		String string = "^$ùµéèá´~/test╥╫a▌æjmlkjzareiuazerpoijmjllkq╥▓Æqsdfqsdfqsdfp^pooppazersìq";
		ReadableContainer<CharBuffer> chars = IOUtils.wrap(string);
		ReadableContainer<ByteBuffer> bytes = IOUtils.unwrap(chars, Charset.forName("UTF-8"));
		ReadableContainer<CharBuffer> charsAgain = IOUtils.wrapReadable(bytes, Charset.forName("UTF-8"));
		assertEquals(string, IOUtils.toString(charsAgain));
	}
}
