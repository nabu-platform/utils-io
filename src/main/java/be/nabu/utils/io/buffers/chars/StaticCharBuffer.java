package be.nabu.utils.io.buffers.chars;

import java.io.IOException;

import be.nabu.utils.io.api.CharBuffer;
import be.nabu.utils.io.api.DuplicatableContainer;
import be.nabu.utils.io.api.PeekableContainer;
import be.nabu.utils.io.api.PositionableContainer;
import be.nabu.utils.io.api.ResettableContainer;

public class StaticCharBuffer implements CharBuffer, ResettableContainer<CharBuffer>, PositionableContainer<CharBuffer>, DuplicatableContainer<CharBuffer, StaticCharBuffer>, PeekableContainer<CharBuffer> {

	private char [] chars;

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
	private StaticCharBuffer parent;
	
	private StaticCharBuffer(StaticCharBuffer parent, boolean duplicateState) {
		this.parent = parent;
		this.writeEnd = parent.writeEnd;
		if (duplicateState)
			readPointer = parent.readPointer;
	}
	
	public StaticCharBuffer(int size) {
		this(new char[size], false);
	}
	
	public StaticCharBuffer(char [] chars, boolean containsData) {
		this(chars, 0, chars.length, containsData);
	}
	
	public StaticCharBuffer(char [] chars, int offset, int length, boolean containsData) {
		this.chars = chars;
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
	public long read(CharBuffer buffer) throws IOException {
		// you don't want to actually read anything
		if (buffer.remainingSpace() == 0) {
			return 0;
		}
		int amountToRead = (int) Math.min(remainingData(), buffer.remainingSpace());
		if (amountToRead > 0) {
			buffer.write(getChars(), readPointer, amountToRead);
			readPointer += amountToRead;
		}
		return amountToRead == 0 && (remainingSpace() == 0 || closed) && remainingData() == 0 ? -1 : amountToRead;
	}

	@Override
	public long write(CharBuffer buffer) throws IOException {
		return parent == null ? buffer.read(this) : 0;
	}

	private char [] getChars() {
		return parent == null ? chars : parent.chars;
	}
	
	@Override
	public int read(char[] chars, int offset, int length) {
		// if you don't want to read anything, just return 0
		if (length == 0)
			return 0;
		// otherwise we try to read something
		length = (int) Math.min(length, remainingData());
		if (length > 0) {
			System.arraycopy(getChars(), readPointer, chars, offset, length);
			readPointer += length;
			return length;
		}
		else
			// if we couldn't read anything and you can't write anything anymore, stop
			return remainingSpace() == 0 || closed ? -1 : length;
	}

	@Override
	public int write(char[] chars, int offset, int length) {
		if (closed) {
			return -1;
		}
		length = (int) Math.min(length, remainingSpace());
		if (length > 0) {
			System.arraycopy(chars, offset, this.chars, writePointer, length);
			writePointer += length;
		}
		return length;
	}
	
	@Override
	public StaticCharBuffer duplicate(boolean duplicateState) {
		return new StaticCharBuffer(this, duplicateState);
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
	public long peek(CharBuffer buffer) throws IOException {
		return buffer.write(duplicate(true));
	}
	
	@Override
	public String toString() {
		return new String(chars, readPointer, writePointer);
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

}
