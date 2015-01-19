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
