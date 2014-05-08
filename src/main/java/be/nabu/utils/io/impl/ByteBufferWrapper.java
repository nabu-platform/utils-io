package be.nabu.utils.io.impl;

import java.nio.ByteBuffer;

import be.nabu.utils.io.api.ByteContainer;
import be.nabu.utils.io.api.LimitedReadableByteContainer;
import be.nabu.utils.io.api.LimitedWritableByteContainer;

public class ByteBufferWrapper implements ByteContainer, LimitedReadableByteContainer, LimitedWritableByteContainer {

	private ByteBuffer buffer;
	
	private boolean closed = false;
	
	private boolean wasReading = false;
	
	public ByteBufferWrapper() {
		this(10240);
	}
	
	public ByteBufferWrapper(int size) {
		this(ByteBuffer.allocate(size), false);
	}
	
	/**
	 * The buffer that is passed in should be ready to be read
	 * This is the default if the bytebuffer was wrapped around a byte array
	 * @param buffer
	 * @param containsData Suppose you are wrapping a byte array in a buffer, there are two possibilities:
	 * - the array already contains actual data that you want to be read. In this case the buffer is ready for reading (position = 0, limit = capacity)
	 * - the array does not contain any data, you want to write to it. If you treat it as a readable buffer, the first move will be to compact() it which means (amongst other things) position is set to limit() + 1
	 * 
	 * Because there i no way to deduce the state of the buffer, you need to pass this along as a boolean
	 */
	public ByteBufferWrapper(ByteBuffer buffer, boolean containsData) {
		this.buffer = buffer;
		wasReading = containsData;
	}
	
	@Override
	public int read(byte[] bytes) {
		return read(bytes, 0, bytes.length);
	}

	@Override
	public int read(byte[] bytes, int offset, int length) {
		if (!wasReading) {
			wasReading = true;
			buffer.flip();
		}
		int readSize = Math.min(length, buffer.remaining());
		buffer.get(bytes, offset, readSize);
		return closed && readSize == 0 ? -1 : readSize;
	}

	@Override
	public int write(byte[] bytes) {
		return write(bytes, 0, bytes.length);
	}

	@Override
	public int write(byte[] bytes, int offset, int length) {
		if (closed)
			throw new IllegalStateException("Can't write to a closed container");
		if (wasReading) {
			wasReading = false;
			buffer.compact();
		}
		length = (int) Math.min(length, buffer.remaining());
		buffer.put(bytes, offset, length);
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
	public long remainingSpace() {
		if (wasReading) {
			wasReading = false;
			buffer.compact();
		}
		return buffer.remaining();
	}

	@Override
	public void flush() {
		// do nothing
	}

}
