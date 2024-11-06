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

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.CharBuffer;
import be.nabu.utils.io.api.Container;
import junit.framework.TestCase;

public class TestFileWrapper extends TestCase {
	public void testFileWrapper() throws IOException {
		File target = File.createTempFile("test", ".txt");
		String testString = "writing some spécial chäractèrs";
		
		Container<ByteBuffer> bytes = IOUtils.wrap(target);
		Container<CharBuffer> chars = IOUtils.wrap(bytes, Charset.forName("UTF-8"));
		
		chars.write(IOUtils.wrap(testString));
		chars.flush();
		
		assertEquals(testString, IOUtils.toString(chars));
		assertTrue(target.exists());
		assertEquals(34, target.length());
		
		target.delete();
	}
	
	public void testBufferedFileWrapper() throws IOException {
		File target = File.createTempFile("test", ".txt");
		String testString = "writing some spécial chäractèrs";
		
		Container<ByteBuffer> bytes = IOUtils.wrap(target);
		bytes = IOUtils.wrap(
			IOUtils.bufferReadable(bytes, IOUtils.newByteBuffer(10, true)),
			IOUtils.bufferWritable(bytes, IOUtils.newByteBuffer(10, true))
		);
		Container<CharBuffer> chars = IOUtils.wrap(bytes, Charset.forName("UTF-8"));
		
		chars.write(IOUtils.wrap(testString));
		chars.flush();
		
		assertEquals(testString, IOUtils.toString(chars));
		assertTrue(target.exists());
		assertEquals(34, target.length());
		
		target.delete();
	}
}
