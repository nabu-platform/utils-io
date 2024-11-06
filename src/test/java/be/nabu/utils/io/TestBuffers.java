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
