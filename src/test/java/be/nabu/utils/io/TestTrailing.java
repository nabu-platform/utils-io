package be.nabu.utils.io;

import java.io.IOException;

import junit.framework.TestCase;
import be.nabu.utils.io.api.CharBuffer;
import be.nabu.utils.io.api.Container;
import be.nabu.utils.io.containers.TrailingContainer;

public class TestTrailing extends TestCase {
	
	public void testTrailing() throws IOException {
		Container<CharBuffer> container = IOUtils.newCharBuffer();
		TrailingContainer<CharBuffer> trailer = new TrailingContainer<CharBuffer>(container, 4);
		container.write(IOUtils.wrap("this is a test"));
		IOUtils.copyChars(trailer, IOUtils.newCharSink());
		assertEquals("test", IOUtils.toString(trailer.getTrailing()));
	}
	
	public void testTrailingPartialReads() throws IOException {
		Container<CharBuffer> container = IOUtils.newCharBuffer();
		TrailingContainer<CharBuffer> trailer = new TrailingContainer<CharBuffer>(container, 4);
		container.write(IOUtils.wrap("this is a test"));
		while (trailer.read(IOUtils.newCharBuffer(2, false)) > 0);
		assertEquals("test", IOUtils.toString(trailer.getTrailing()));
	}
	
	public void testRepeatedTrailing() throws IOException {
		Container<CharBuffer> container = IOUtils.newCharBuffer();
		TrailingContainer<CharBuffer> trailer = new TrailingContainer<CharBuffer>(container, 4);
		container.write(IOUtils.wrap("this is a test"));
		IOUtils.copyChars(trailer, IOUtils.newCharSink());
		container.write(IOUtils.wrap("just making sure"));
		IOUtils.copyChars(trailer, IOUtils.newCharSink());
		assertEquals("sure", IOUtils.toString(trailer.getTrailing()));
	}
}
