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
import java.nio.charset.Charset;

import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.CharBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import junit.framework.TestCase;

public class TestUnwrap extends TestCase {
	
	public void testUnwrap() throws IOException {
		ReadableContainer<CharBuffer> chars = IOUtils.wrap("tést");
		ReadableContainer<ByteBuffer> bytes = IOUtils.unwrap(chars, Charset.forName("UTF-8"));
		byte [] array = IOUtils.toBytes(bytes);
		assertEquals(5, array.length);
		assertEquals(116, (int) array[0] & 0xff);
		// c3 a9 = é in UTF-8
		assertEquals(0xc3, (int) array[1] & 0xff);
		assertEquals(0xa9, (int) array[2] & 0xff);
		assertEquals(115, (int) array[3] & 0xff);
		assertEquals(116, (int) array[4] & 0xff);
	}
	
	public void testComplexUnwrap() throws IOException {
		String string = "^$ùµéèá´~/test╥╫a▌æjmlkjzareiuazerpoijmjllkq╥▓Æqsdfqsdfqsdfp^pooppazersìq";
		ReadableContainer<CharBuffer> chars = IOUtils.wrap(string);
		ReadableContainer<ByteBuffer> bytes = IOUtils.unwrap(chars, Charset.forName("UTF-8"));
		ReadableContainer<CharBuffer> charsAgain = IOUtils.wrapReadable(bytes, Charset.forName("UTF-8"));
		assertEquals(string, IOUtils.toString(charsAgain));
	}
}
