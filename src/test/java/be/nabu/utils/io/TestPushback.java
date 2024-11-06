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
