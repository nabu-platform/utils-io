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
		if (buffer.remainingSpace() == 0) {
			return 0;
		}
		int amountToRead = (int) Math.min(remainingData(), buffer.remainingSpace());
		if (amountToRead > 0) {
			buffer.write(getBytes(), readPointer, amountToRead);
			readPointer += amountToRead;
		}
		return amountToRead;
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
			return remainingSpace() == 0 ? -1 : length;
	}

	@Override
	public int write(byte[] bytes, int offset, int length) {
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
		// do nothing
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
