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

package be.nabu.utils.io.buffers.bytes;

import java.io.IOException;

import be.nabu.utils.io.api.ByteBuffer;

public class NioByteBufferWrapper implements ByteBuffer {

	private java.nio.ByteBuffer buffer;
	
	private byte [] copyBuffer = new byte[4096];
	
	private boolean closed = false;
	
	private boolean wasReading = false;
	
	public NioByteBufferWrapper() {
		this(10240);
	}
	
	public NioByteBufferWrapper(int size) {
		this(java.nio.ByteBuffer.allocate(size), false);
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
	public NioByteBufferWrapper(java.nio.ByteBuffer buffer, boolean containsData) {
		this.buffer = buffer;
		wasReading = containsData;
	}

	@Override
	public int read(byte[] bytes, int offset, int length) {
		return read(bytes, offset, length, true);
	}
	
	private int read(byte[] bytes, int offset, int length, boolean updateState) {
		if (!wasReading) {
			wasReading = true;
			buffer.flip();
		}
		int readSize = Math.min(length, buffer.remaining());
		java.nio.ByteBuffer source = updateState ? buffer : buffer.duplicate();
		source.get(bytes, offset, readSize);
		return closed && readSize == 0 ? -1 : readSize;
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

	@Override
	public long read(ByteBuffer buffer) throws IOException {
		return read(buffer, true);
	}
	private long read(ByteBuffer target, boolean updateState) throws IOException {
		if (!wasReading) {
			wasReading = true;
			buffer.flip();
		}
		int read = (int) Math.min(target.remainingSpace(), remainingData());
		if (read == 0)
			return closed ? -1 : read;
		java.nio.ByteBuffer source = updateState ? this.buffer : this.buffer.duplicate();
		int totalRead = 0;
		while (read > 0) {
			int readSize = Math.min(copyBuffer.length, read);
			source.get(copyBuffer, 0, readSize);
			target.write(copyBuffer, 0, readSize);
			read -= readSize;
			totalRead += readSize;
		}
		return totalRead == 0 && closed ? -1 : totalRead;
	}

	@Override
	public long write(ByteBuffer buffer) throws IOException {
		if (wasReading) {
			wasReading = false;
			this.buffer.compact();
		}
		long total = 0;
		while(buffer.remainingData() > 0) {
			int read = (int) Math.min(buffer.remainingData(), remainingSpace());
			if (read == 0)
				break;
			read = Math.min(read, copyBuffer.length);
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
		return read(getFactory().newSink(amount));
	}
	
	@Override
	public int read(byte[] bytes) throws IOException {
		return read(bytes, 0, bytes.length);
	}

	@Override
	public int write(byte[] bytes) throws IOException {
		return write(bytes, 0, bytes.length);
	}
	@Override
	public ByteBufferFactory getFactory() {
		return ByteBufferFactory.getInstance();
	}

	@Override
	public long peek(ByteBuffer target) throws IOException {
		return read(target, false);
	}
}
