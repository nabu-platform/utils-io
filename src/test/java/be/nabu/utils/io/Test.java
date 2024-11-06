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
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.CharBuffer;
import be.nabu.utils.io.api.Container;
import be.nabu.utils.io.api.MarkableContainer;
import be.nabu.utils.io.api.PushbackContainer;
import be.nabu.utils.io.buffers.bytes.DynamicByteBuffer;
import be.nabu.utils.io.buffers.bytes.NioByteBufferWrapper;
import be.nabu.utils.io.buffers.chars.DynamicCharBuffer;
import junit.framework.TestCase;
import static be.nabu.utils.io.IOUtils.*;

public class Test extends TestCase {
	
	public void testSkip() throws IOException {
		CharBuffer container = new DynamicCharBuffer();
		container.write("test".toCharArray());
		container.skip(2);
		assertEquals("st", IOUtils.toString(container));
		container = new DynamicCharBuffer(4);
		container.write("this is a slightly larger test".toCharArray());
		container.skip(26);
		assertEquals("test", IOUtils.toString(container));
	}
	
	public void testPushback() throws IOException {
		CharBuffer container = newCharBuffer();
		PushbackContainer<CharBuffer> pushback = pushback(container);

		container.write("test".toCharArray());
		assertEquals("test", IOUtils.toString(pushback));
		
		pushback.pushback(IOUtils.wrap("ge"));
		assertEquals("ge", IOUtils.toString(pushback));
		
		container.write("test".toCharArray());
		pushback.pushback(IOUtils.wrap("ge"));
		assertEquals("getest", IOUtils.toString(pushback));
	}
	
	public void testByteBufferWrapper() throws IOException {
		String string = "test";
		Container<CharBuffer> container = wrap(new NioByteBufferWrapper(5), Charset.forName("ASCII"));
		
		// test single write/read (written 4/5)
		container.write(wrap(string));
		assertEquals(string, IOUtils.toString(container));

		// test another single write/read (written 4/5) to see if it is cleaned up properly
		container.write(wrap(string));
		assertEquals(string, IOUtils.toString(container));
		// write more than the fixed byte buffer can handle
		try {
			// writing 4/5
			container.write(wrap(string));
			// actually wrote 5/5 and buffered 3
			container.write(wrap(string));
			// wrote 5/5 and buffered 7
			container.write(wrap(string));
			container.flush();
			fail("The backing bytebuffer is full so we expect an error when flushing the data");
		}
		catch (IOException e) {
			// we have 7 buffered but the backend is still full so we can't flush it
			assertEquals("0/7", e.getMessage().replaceAll(".*?([0-9/]+).*", "$1"));
		}
		
		// if we read it out we have to read 5 bytes
		assertEquals(string + string.substring(0, 1), IOUtils.toString(container));
		
		// we still can't flush the full 7 bytes
		try {
			container.flush();
		}
		catch (IOException e) {
			// we have 7 buffered but the backend is still full so we still can't flush it
			assertEquals("5/7", e.getMessage().replaceAll(".*?([0-9/]+).*", "$1"));
		}
		// should contain "estte"
		assertEquals(string.substring(1) + string.substring(0, 2), IOUtils.toString(container));
		
		// write "te", so it should now contain "stte"
		container.write(wrap(string.substring(0, 2)));
		
		assertEquals(string.substring(2) + string.substring(0, 2), IOUtils.toString(container));
		
		// flushing should not be a problem because it's empty
		container.flush();
		
		// create a larger buffer to write more data
		container = wrap(new NioByteBufferWrapper(8), Charset.forName("ASCII"));
		container.write(wrap(string));
		container.write(wrap(string));
		assertEquals(string + string, IOUtils.toString(container));
		
		// make sure once everything is read, the next read returns nothing
		assertEquals("", IOUtils.toString(container));
	}
	
	public void testReadOrWriteByteBuffer() throws UnsupportedEncodingException {
		assertEquals(4, IOUtils.wrap("test".getBytes("ASCII"), true).remainingData());
	}
	
	public void testReader() throws IOException {
		String string = "this is a test";
		CharBuffer container = newCharBuffer();
		container.write(string.toCharArray());
		container.close();
		int read = 0;
		char [] buffer = new char[1024];
		Reader reader = toReader(container);
		StringBuilder builder = new StringBuilder();
		while ((read = reader.read(buffer)) != -1)
			builder.append(new String(buffer, 0, read));
		assertEquals(string, builder.toString());
	}
	
	public void testSmallReader() throws IOException {
		String string = "this is a test";
		CharBuffer container = newCharBuffer();
		container.write(string.toCharArray());
		container.close();
		int read = 0;
		char [] buffer = new char[3];
		Reader reader = toReader(container);
		StringBuilder builder = new StringBuilder();
		while ((read = reader.read(buffer)) != -1)
			builder.append(new String(buffer, 0, read));
		assertEquals(string, builder.toString());
	}


	public void testMarkable() throws IOException {
		DynamicByteBuffer container = new DynamicByteBuffer();
		container.write("test".getBytes());
		
		container.mark();
		assertEquals("test", new String(IOUtils.toBytes(container)));
		container.reset();
		assertEquals("test", new String(IOUtils.toBytes(container)));
		container.unmark();
		try {
			container.reset();
			fail("The reset should fail because there is no mark");
		}
		catch (IllegalStateException e) {
			// expected behavior, there is no mark
		}
		container = new DynamicByteBuffer();
		container.write("test".getBytes());
		container.mark();
		container.reset();
		assertEquals("test", new String(IOUtils.toBytes(container)));
		container.close();
	}
	
	public void testLimitedMarkable() throws IOException {
		MarkableContainer<CharBuffer> content = mark(IOUtils.wrap("this is a test"), 4);
		content.mark();
		char [] chars = new char[7];
		// read the first 4 chars (= this)
		content.read(wrap(chars, 0, 4, false));
		assertEquals("this", new String(chars, 0, 4));
		// reset to the start
		content.reset();
		// read 7 chars
		content.read(wrap(chars, false));
		assertEquals("this is", new String(chars));
		try {
			content.reset();
			fail("The container can not be reset past 4");
		}
		catch(Exception e) {
			// expected
		}
		assertEquals(" a test", IOUtils.toString(content));
	}
	
	public void test1() throws IOException {
		String string = "something new é!";
		
		ByteBuffer bytes = newByteBuffer();

		Container<CharBuffer> container = IOUtils.wrap(bytes, Charset.forName("UTF-8"));
		container.write(wrap(string));
		container.close();
		
		assertEquals(string, IOUtils.toString(container));
	}
}
