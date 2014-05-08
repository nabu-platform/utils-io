package be.nabu.utils.io;

import java.io.IOException;

import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.Container;
import junit.framework.TestCase;

public class TestBuffers extends TestCase {
	public void testOutputBuffering() throws IOException {
		Container<ByteBuffer> container = IOUtils.newByteBuffer();
		// for this test the buffer must be bigger then the amount we are writing to it
		container = IOUtils.wrap(
			container,
			IOUtils.bufferWritable(container, IOUtils.newByteBuffer(5, true))
		);
		container.write(IOUtils.wrap("test".getBytes(), true));
		// check that nothing has been flushed yet
		assertEquals(0, IOUtils.toBytes(container).length);
		container.flush();
		assertEquals("test", new String(IOUtils.toBytes(container)));
	}
	
	public void testInputBuffering() throws IOException {
		Container<ByteBuffer> container = IOUtils.newByteBuffer();
		container = IOUtils.wrap(
			IOUtils.bufferReadable(container, IOUtils.newByteBuffer(2, true)),
			container
		);
		container.write(IOUtils.wrap("test".getBytes(), true));
		assertEquals("test", new String(IOUtils.toBytes(container)));
	}
}
