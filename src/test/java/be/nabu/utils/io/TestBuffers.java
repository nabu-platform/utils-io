package be.nabu.utils.io;

import be.nabu.utils.io.api.ByteContainer;
import junit.framework.TestCase;

public class TestBuffers extends TestCase {
	public void testOutputBuffering() {
		ByteContainer container = IOUtils.newByteContainer();
		container = IOUtils.wrap(
			container,
			IOUtils.bufferOutput(container)
		);
		container.write("test".getBytes());
		assertEquals(0, IOUtils.toBytes(container).length);
		container.flush();
		assertEquals("test", new String(IOUtils.toBytes(container)));
	}
	
	public void testInputBuffering() {
		ByteContainer container = IOUtils.newByteContainer();
		container = IOUtils.wrap(
			IOUtils.bufferInput(container, 2),
			container
		);
		container.write("test".getBytes());
		assertEquals("test", new String(IOUtils.toBytes(container)));
	}
}
