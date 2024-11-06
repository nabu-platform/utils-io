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
