package be.nabu.utils.io;

import java.io.IOException;

import be.nabu.utils.io.api.CharBuffer;
import be.nabu.utils.io.api.PushbackContainer;
import junit.framework.TestCase;

public class TestPushback extends TestCase {
	public void testPushback() throws IOException {
		CharBuffer buffer = IOUtils.newCharBuffer();
		PushbackContainer<CharBuffer> pushback = IOUtils.pushback(buffer);
		buffer.write(IOUtils.wrap("this is a test"));
		
		CharBuffer read = IOUtils.newCharBuffer(4, true);
		pushback.read(read);
		assertEquals("this", IOUtils.toString(read));
		
		pushback.pushback(IOUtils.wrap("that"));
		pushback.read(read);
		assertEquals("that", IOUtils.toString(read));
		
		pushback.pushback(IOUtils.wrap("something"));
		pushback.read(read);
		assertEquals("some", IOUtils.toString(read));
		
		pushback.pushback(IOUtils.wrap("this"));
		pushback.read(read);
		assertEquals("this", IOUtils.toString(read));
	}
}
