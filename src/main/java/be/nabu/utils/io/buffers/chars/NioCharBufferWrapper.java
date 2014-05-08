package be.nabu.utils.io.buffers.chars;

import java.io.IOException;

import be.nabu.utils.io.api.CharBuffer;

public class NioCharBufferWrapper implements CharBuffer {

	private java.nio.CharBuffer buffer;
	
	private char [] copyBuffer = new char[4096];
	
	private boolean closed = false;
	
	private boolean wasReading = false;
	
	public NioCharBufferWrapper() {
		this(10240);
	}
	
	public NioCharBufferWrapper(int size) {
		this(java.nio.CharBuffer.allocate(size), false);
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
	public NioCharBufferWrapper(java.nio.CharBuffer buffer, boolean containsData) {
		this.buffer = buffer;
		wasReading = containsData;
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
	public int write(char[] chars, int offset, int length) {
		if (closed)
			throw new IllegalStateException("Can't write to a closed container");
		if (wasReading) {
			wasReading = false;
			buffer.compact();
		}
		length = (int) Math.min(length, buffer.remaining());
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

	@Override
	public long read(CharBuffer buffer) throws IOException {
		return read(buffer, true);
	}
	private long read(CharBuffer target, boolean updateState) throws IOException {
		int read = (int) Math.min(target.remainingSpace(), remainingData());
		int position = buffer.position();
		int limit = buffer.limit();
		if (read == 0)
			return closed ? -1 : read;
		buffer.get(copyBuffer, 0, read);
		if (!updateState) {
			buffer.position(position);
			buffer.limit(limit);
		}
		target.write(copyBuffer, 0, read);
		return read;
	}
	
	@Override
	public long write(CharBuffer buffer) throws IOException {
		long total = 0;
		while(buffer.remainingData() > 0) {
			int read = (int) Math.min(buffer.remainingData(), remainingSpace());
			if (read == 0)
				break;
			buffer.read(copyBuffer, 0, read);
			this.buffer.put(copyBuffer, 0, read);
			total += read;
		}
		return total;
	}

	@Override
	public void truncate() {
		buffer.clear();
	}

	@Override
	public long skip(long amount) throws IOException {
		return write(getFactory().newSink(amount));
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
		return CharBufferFactory.getInstance();
	}

	@Override
	public long peek(CharBuffer target) throws IOException {
		return read(target, false);
	}

}
