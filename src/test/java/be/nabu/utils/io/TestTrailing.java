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
