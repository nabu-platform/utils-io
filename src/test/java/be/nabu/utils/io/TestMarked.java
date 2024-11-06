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

import be.nabu.utils.io.api.CharBuffer;
import be.nabu.utils.io.buffers.LimitedCharBuffer;
import be.nabu.utils.io.containers.LimitedMarkableContainer;
import junit.framework.TestCase;

public class TestMarked extends TestCase {
	public void testMark() throws IOException {
		LimitedMarkableContainer<CharBuffer> marked = new LimitedMarkableContainer<CharBuffer>(IOUtils.wrap("abcdef"), 0);
		marked.mark();
		
		CharBuffer buffer = IOUtils.newCharBuffer();
		marked.read(new LimitedCharBuffer(buffer,  null, 3l));
		assertEquals("abc", IOUtils.toString(buffer));
		
		marked.reset();
		marked.read(new LimitedCharBuffer(buffer, null, 3l));
		assertEquals("abc", IOUtils.toString(buffer));
		
		marked.pushback(IOUtils.wrap("c"));
		marked.remark();
		marked.read(new LimitedCharBuffer(buffer, null, 3l));
		assertEquals("cde", IOUtils.toString(buffer));
		
		marked.reset();
		marked.read(new LimitedCharBuffer(buffer, null, 3l));
		assertEquals("cde", IOUtils.toString(buffer));

		marked.reset();
		marked.read(new LimitedCharBuffer(buffer, null, 1l));
		assertEquals("c", IOUtils.toString(buffer));
		
		marked.pushback(IOUtils.wrap("c"));
		marked.remark();
		marked.read(new LimitedCharBuffer(buffer, null, 3l));
		assertEquals("cde", IOUtils.toString(buffer));
		
		marked.reset();
		marked.read(new LimitedCharBuffer(buffer, null, 3l));
		assertEquals("cde", IOUtils.toString(buffer));
	}
}
