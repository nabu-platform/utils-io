package be.nabu.utils.io.buffers.chars;

import java.io.IOException;

import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.CharBuffer;
import be.nabu.utils.io.buffers.DynamicBuffer;

public class DynamicCharBuffer extends DynamicBuffer<CharBuffer, StaticCharBuffer> implements CharBuffer {

	public DynamicCharBuffer() {
		super(CharBufferFactory.getInstance());
	}

	public DynamicCharBuffer(int bufferSize) {
		super(CharBufferFactory.getInstance(), bufferSize);
	}

	@Override
	protected StaticCharBuffer newBuffer(int size) {
		return new StaticCharBuffer(size);
	}

	@Override
	public int read(char[] chars, int offset, int length) throws IOException {
		return (int) read(IOUtils.wrap(chars, offset, length, false));
	}

	@Override
	public int write(char[] chars, int offset, int length) throws IOException {
		return (int) write(IOUtils.wrap(chars, offset, length, true));
	}
	@Override
	public int read(char[] chars) throws IOException {
		return read(chars, 0, chars.length);
	}

	@Override
	public int write(char[] chars) throws IOException {
		return write(chars, 0, chars.length);
	}
	@Override
	public CharBufferFactory getFactory() {
		return (CharBufferFactory) super.getFactory();
	}

}
