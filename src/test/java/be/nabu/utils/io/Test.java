package be.nabu.utils.io;

import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import junit.framework.TestCase;
import be.nabu.utils.io.api.ByteContainer;
import be.nabu.utils.io.api.CharContainer;
import be.nabu.utils.io.api.IORuntimeException;
import be.nabu.utils.io.api.LimitedReadableByteContainer;
import be.nabu.utils.io.api.MarkableCharContainer;
import be.nabu.utils.io.api.PushbackCharContainer;
import be.nabu.utils.io.impl.BackedCharContainer;
import be.nabu.utils.io.impl.ByteBufferWrapper;
import be.nabu.utils.io.impl.DynamicByteContainer;
import be.nabu.utils.io.impl.DynamicCharContainer;

public class Test extends TestCase {
	
	public void testSkip() {
		DynamicCharContainer container = new DynamicCharContainer();
		container.write("test".toCharArray());
		container.skip(2);
		assertEquals("st", IOUtils.toString(container));
		
		container = new DynamicCharContainer(4);
		container.write("this is a slightly larger test".toCharArray());
		container.skip(26);
		assertEquals("test", IOUtils.toString(container));
		
		// use a non-skippable container
		CharContainer container2 = IOUtils.wrap("test".toCharArray(), true);
		IOUtils.skip(container2, 2);
		assertEquals("st", IOUtils.toString(container2));
	}
	
	public void testPushback() {
		CharContainer container = IOUtils.newCharContainer();
		PushbackCharContainer pushback = IOUtils.wrapPushback(container);
		
		container.write("test".toCharArray());
		assertEquals("test", IOUtils.toString(pushback));
		
		pushback.pushback("ge".toCharArray());
		assertEquals("ge", IOUtils.toString(pushback));
		
		container.write("test".toCharArray());
		pushback.pushback("ge".toCharArray());
		assertEquals("getest", IOUtils.toString(pushback));
	}
	
	public void testByteBufferWrapper() {
		String string = "test";
		CharContainer container = new BackedCharContainer(new ByteBufferWrapper(5), Charset.forName("ASCII"));
		
		// test single write/read (written 4/5)
		container.write(string.toCharArray());
		assertEquals(string, IOUtils.toString(container));

		// test another single write/read (written 4/5) to see if it is cleaned up properly
		container.write(string.toCharArray());
		assertEquals(string, IOUtils.toString(container));
		
		// write more than the fixed byte buffer can handle
		try {
			// writing 4/5
			container.write(string.toCharArray());
			// actually wrote 5/5 and buffered 3
			container.write(string.toCharArray());
			// wrote 5/5 and buffered 7
			container.write(string.toCharArray());
			container.flush();
			fail("The backing bytebuffer is full so we expect an error when flushing the data");
		}
		catch (IORuntimeException e) {
			// we have 7 buffered but the backend is still full so we can't flush it
			assertEquals("0/7", e.getMessage().replaceAll(".*?([0-9/]+).*", "$1"));
		}
		
		// if we read it out we have to read 5 bytes
		assertEquals(string + string.substring(0, 1), IOUtils.toString(container));
		
		// we still can't flush the full 7 bytes
		try {
			container.flush();
		}
		catch (IORuntimeException e) {
			// we have 7 buffered but the backend is still full so we still can't flush it
			assertEquals("5/7", e.getMessage().replaceAll(".*?([0-9/]+).*", "$1"));
		}
		// should contain "estte"
		assertEquals(string.substring(1) + string.substring(0, 2), IOUtils.toString(container));
		
		// write "te", so it should now contain "stte"
		container.write(string.substring(0, 2).toCharArray());
		
		assertEquals(string.substring(2) + string.substring(0, 2), IOUtils.toString(container));
		
		// flushing should not be a problem because it's empty
		container.flush();
		
		// create a larger buffer to write more data
		container = new BackedCharContainer(new ByteBufferWrapper(8), Charset.forName("ASCII"));
		container.write(string.toCharArray());
		container.write(string.toCharArray());
		assertEquals(string + string, IOUtils.toString(container));
		
		// make sure once everything is read, the next read returns nothing
		assertEquals("", IOUtils.toString(container));
	}
	
	public void testReadOrWriteByteBuffer() throws UnsupportedEncodingException {
		LimitedReadableByteContainer container = (LimitedReadableByteContainer) IOUtils.wrap("test".getBytes("ASCII"), true);
		assertEquals(4, container.remainingData());
	}
	
	public void testReader() throws IOException {
		String string = "this is a test";
		CharContainer container = IOUtils.newCharContainer();
		container.write(string.toCharArray());
		int read = 0;
		char [] buffer = new char[1024];
		Reader reader = IOUtils.toReader(container);
		StringBuilder builder = new StringBuilder();
		while ((read = reader.read(buffer)) != -1)
			builder.append(new String(buffer, 0, read));
		assertEquals(string, builder.toString());
	}

	public void testMarkable() {
		DynamicByteContainer container = new DynamicByteContainer();
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
		container = new DynamicByteContainer();
		container.write("test".getBytes());
		container.mark();
		container.moveMark(2);
		container.reset();
		assertEquals("st", new String(IOUtils.toBytes(container)));
		container.close();
	}
	
	public void testLimitedMarkable() {
		MarkableCharContainer content = IOUtils.wrapMarkable(IOUtils.wrap("this is a test"), 4);
		content.mark();
		char [] chars = new char[7];
		content.read(chars, 0, 4);
		assertEquals("this", new String(chars, 0, 4));
		content.reset();
		content.read(chars);
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
		
		ByteContainer bytes = IOUtils.newByteContainer();
		// create a composed
		bytes = IOUtils.wrap(bytes, bytes);

		CharContainer container = IOUtils.wrap(bytes, Charset.forName("UTF-8"));
		container.write(string.toCharArray());
		container.close();
		
		assertEquals(string, IOUtils.toString(container));
	}
}
