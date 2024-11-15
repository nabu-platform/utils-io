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

import be.nabu.utils.io.api.BufferFactory;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.DuplicatableContainer;
import be.nabu.utils.io.api.PeekableContainer;
import be.nabu.utils.io.api.PositionableContainer;
import be.nabu.utils.io.api.ResettableContainer;

public class StaticByteBuffer implements ByteBuffer, ResettableContainer<ByteBuffer>, PositionableContainer<ByteBuffer>, DuplicatableContainer<ByteBuffer, StaticByteBuffer>, PeekableContainer<ByteBuffer> {

	private byte [] bytes;

	private int writePointer = 0, 
		readPointer = 0;
	
	/**
	 * The end of the buffer where we can write to
	 */
	private int writeEnd = -1;
	private int readStart = 0;
	
	private boolean closed;
	
	/**
	 * If the buffer has a parent, it is a read-only view of the parent
	 */
	private StaticByteBuffer parent;
	
	private StaticByteBuffer(StaticByteBuffer parent, boolean duplicateState) {
		this.parent = parent;
		if (duplicateState)
			readPointer = parent.readPointer;
	}
	
	public StaticByteBuffer(int size) {
		this(new byte[size], false);
	}
	
	public StaticByteBuffer(byte [] bytes, boolean containsData) {
		this(bytes, 0, bytes.length, containsData);
	}
	
	public StaticByteBuffer(byte [] bytes, int offset, int length, boolean containsData) {
		if (bytes.length < offset + length) {
			throw new IllegalArgumentException("Can not wrap byte array with length " + bytes.length + " and an offset of " + offset + " with a length of " + length);
		}
		this.bytes = bytes;
		this.readPointer = offset;
		this.writePointer = containsData ? offset + length : offset;
		this.writeEnd = offset + length;
		this.readStart = offset;
	}
	
	@Override
	public long remainingData() {
		return getWritePointer() - readPointer;
	}
	
	private int getWritePointer() {
		return parent == null ? writePointer : parent.writePointer;
	}

	@Override
	public long remainingSpace() {
		// can not write if the array is owned by the parent
		return parent != null ? 0 : writeEnd - writePointer;
	}

	@Override
	public void truncate() {
		writePointer = 0;
		readPointer = 0;
	}

	@Override
	public long skip(long amount) {
		amount = Math.min(amount, remainingData());
		readPointer += amount;
		return amount;
	}

	@Override
	public long read(ByteBuffer buffer) throws IOException {
		// you don't want to actually read anything
		long remainingSpace = buffer.remainingSpace();
		if (remainingSpace == 0) {
			return 0;
		}
		int amountToRead = (int) Math.min(remainingData(), remainingSpace);
		if (amountToRead > 0) {
			buffer.write(getBytes(), readPointer, amountToRead);
			readPointer += amountToRead;
		}
		return amountToRead == 0 && (remainingSpace() == 0 || closed) && remainingData() == 0 ? -1 : amountToRead;
	}

	@Override
	public long write(ByteBuffer buffer) throws IOException {
		return parent == null ? buffer.read(this) : 0;
	}

	private byte [] getBytes() {
		return parent == null ? bytes : parent.bytes;
	}

	@Override
	public int read(byte[] bytes, int offset, int length) {
		// if you don't want to read anything, just return 0
		if (length == 0)
			return 0;
		// otherwise we try to read something
		length = (int) Math.min(length, remainingData());
		if (length > 0) {
			System.arraycopy(getBytes(), readPointer, bytes, offset, length);
			readPointer += length;
			return length;
		}
		else
			// if we couldn't read anything and you can't write anything anymore, stop
			return remainingSpace() == 0 || closed ? -1 : length;
	}

	@Override
	public int write(byte[] bytes, int offset, int length) {
		if (closed) {
			return -1;
		}
		length = (int) Math.min(length, remainingSpace());
		if (length > 0) {
			System.arraycopy(bytes, offset, this.bytes, writePointer, length);
			writePointer += length;
		}
		return length;
	}
	
	@Override
	public StaticByteBuffer duplicate(boolean duplicateState) {
		return new StaticByteBuffer(this, duplicateState);
	}
	
	@Override
	public void close() throws IOException {
		closed = true;
	}

	@Override
	public void flush() throws IOException {
		// do nothing
	}

	@Override
	public void reset() {
		readPointer = readStart;
	}

	@Override
	public long position() {
		return readPointer;
	}

	@Override
	public long peek(ByteBuffer buffer) throws IOException {
		return buffer.write(duplicate(true));
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
	public BufferFactory<ByteBuffer> getFactory() {
		return ByteBufferFactory.getInstance();
	}
}
