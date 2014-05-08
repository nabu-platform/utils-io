package be.nabu.utils.io.impl;

import java.nio.CharBuffer;

import be.nabu.utils.io.api.CharContainer;
import be.nabu.utils.io.api.LimitedReadableCharContainer;
import be.nabu.utils.io.api.LimitedWritableCharContainer;

public class CharBufferWrapper implements CharContainer, LimitedReadableCharContainer, LimitedWritableCharContainer {

	private CharBuffer buffer;
	
	private boolean closed = false;
	
	private boolean wasReading = false;
	
	public CharBufferWrapper() {
		this(10240);
	}
	
	public CharBufferWrapper(int size) {
		this(CharBuffer.allocate(size), false);
	}
	
	public CharBufferWrapper(CharBuffer buffer, boolean containsData) {
		this.buffer = buffer;
		this.wasReading = containsData;
	}
	
	@Override
	public int read(char[] chars) {
		return read(chars, 0, chars.length);
	}

	@Override
	public int read(char[] chars, int offset, int length) {
		if (!wasReading) {
			wasReading = true;
			buffer.flip();
		}
		int readSize = Math.min(length, buffer.remaining());
		buffer.get(chars, offset, readSize);
		return closed && readSize == 0 ? -1 : readSize;
	}

	@Override
	public int write(char[] chars) {
		return write(chars, 0, chars.length);
	}

	@Override
	public int write(char[] chars, int offset, int length) {
		if (closed)
			throw new IllegalStateException("Can't write to a closed container");
		if (wasReading) {
			wasReading = false;
			buffer.compact();
		}
		buffer.put(chars, offset, length);
		return length;
	}

	@Override
	public void close() {
		closed = true;
	}

	@Override
	public long remainingData() {
		if (!wasReading) {
			wasReading = true;
			buffer.flip();
		}
		return buffer.remaining();
	}

	@Override
	public void flush() {
		// do nothing
	}

	@Override
	public long remainingSpace() {
		if (wasReading) {
			wasReading = false;
			buffer.compact();
		}
		return buffer.remaining();
	}

}
