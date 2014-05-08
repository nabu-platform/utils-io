package be.nabu.utils.io;

import java.nio.charset.Charset;

import be.nabu.utils.io.api.ReadableByteContainer;
import be.nabu.utils.io.api.ReadableCharContainer;
import junit.framework.TestCase;

public class TestUnwrap extends TestCase {
	
	public void testUnwrap() {
		ReadableCharContainer chars = IOUtils.wrap("tést");
		ReadableByteContainer bytes = IOUtils.unwrap(chars, Charset.forName("UTF-8"));
		byte [] array = IOUtils.toBytes(bytes);
		assertEquals(5, array.length);
		assertEquals(116, (int) array[0] & 0xff);
		// c3 a9 = é in UTF-8
		assertEquals(0xc3, (int) array[1] & 0xff);
		assertEquals(0xa9, (int) array[2] & 0xff);
		assertEquals(115, (int) array[3] & 0xff);
		assertEquals(116, (int) array[4] & 0xff);
	}
	
	public void testComplexUnwrap() {
		String string = "^$ùµéèá´~/test╥╫a▌æjmlkjzareiuazerpoijmjllkq╥▓Æqsdfqsdfqsdfp^pooppazersìq";
		ReadableCharContainer chars = IOUtils.wrap(string);
		ReadableByteContainer bytes = IOUtils.unwrap(chars, Charset.forName("UTF-8"));
		ReadableCharContainer charsAgain = IOUtils.wrap(bytes, Charset.forName("UTF-8"));
		assertEquals(string, IOUtils.toString(charsAgain));
	}
}
