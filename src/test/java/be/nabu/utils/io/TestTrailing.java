package be.nabu.utils.io;

import be.nabu.utils.io.api.CharContainer;
import be.nabu.utils.io.impl.TrailingCharContainer;
import junit.framework.TestCase;

public class TestTrailing extends TestCase {
	public void testTrailing() {
		CharContainer container = IOUtils.newCharContainer();
		TrailingCharContainer trailer = new TrailingCharContainer(container, 4);
		container.write("this is a test".toCharArray());
		IOUtils.copy(trailer, IOUtils.newCharSink());
		assertEquals("test", IOUtils.toString(trailer.getTrailing()));
	}
	
	public void testTrailingPartialReads() {
		CharContainer container = IOUtils.newCharContainer();
		@SuppressWarnings("resource")
		TrailingCharContainer trailer = new TrailingCharContainer(container, 4);
		container.write("this is a test".toCharArray());
		char [] buffer = new char[2];
		while (trailer.read(buffer) > 0);
		assertEquals("test", IOUtils.toString(trailer.getTrailing()));
	}
	
	public void testRepeatedTrailing() {
		CharContainer container = IOUtils.newCharContainer();
		TrailingCharContainer trailer = new TrailingCharContainer(container, 4);
		container.write("this is a test".toCharArray());
		IOUtils.copy(trailer, IOUtils.newCharSink());
		container.write("just making sure".toCharArray());
		IOUtils.copy(trailer, IOUtils.newCharSink());
		assertEquals("sure", IOUtils.toString(trailer.getTrailing()));
	}
}
