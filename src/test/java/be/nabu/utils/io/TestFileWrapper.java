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
