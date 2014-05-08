package be.nabu.utils.io;

import java.io.IOException;

import be.nabu.utils.io.buffers.bytes.CyclicByteBuffer;
import junit.framework.TestCase;

public class TestCyclic extends TestCase {
	public void testCyclic() throws IOException {
		CyclicByteBuffer container = new CyclicByteBuffer(2);
		assertEquals(2, container.remainingSpace());
		assertEquals(0, container.remainingData());
		
		assertEquals(2, container.write("tes".getBytes()));
		assertEquals(0, container.remainingSpace());
		assertEquals(2, container.remainingData());
		assertEquals("te", new String(IOUtils.toBytes(container)));
		assertEquals(2, container.remainingSpace());
		assertEquals(0, container.remainingData());
		
		assertEquals(1, container.write("s".getBytes()));
		assertEquals(1, container.remainingSpace());
		assertEquals(1, container.remainingData());
		assertEquals("s", new String(IOUtils.toBytes(container)));
		assertEquals(2, container.remainingSpace());
		assertEquals(0, container.remainingData());
		
		assertEquals(2, container.write("te".getBytes()));
		assertEquals(0, container.remainingSpace());
		assertEquals(2, container.remainingData());
		assertEquals("te", new String(IOUtils.toBytes(container)));
		
		// test skip
		assertEquals(2, container.write("te".getBytes()));
		assertEquals(1, container.skip(1));
		assertEquals("e", new String(IOUtils.toBytes(container)));
	}
}
