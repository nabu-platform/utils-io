package be.nabu.utils.io.buffers.chars;

import java.io.IOException;

import be.nabu.utils.io.api.CharBuffer;
import be.nabu.utils.io.api.PeekableContainer;

public class CyclicCharBuffer implements CharBuffer, PeekableContainer<CharBuffer> {
	
	private char [] buffer;
	
	private int writePointer = 0, 
		readPointer = -1;
	
	/**
	 * If the write pointer cycles to the start, it is set to true
	 * If the read pointer cycles to the start, it is set to false
	 */
	private boolean cycled = false;
	
	public CyclicCharBuffer(int size) {
		this.buffer = new char[size];
	}
	
	public CyclicCharBuffer(char [] chars) {
		this.buffer = chars;
	}
	
	public CyclicCharBuffer(char [] chars, int offset, int length) {
		this.buffer = chars;
		readPointer = offset;
		writePointer = offset + length;
	}
	
	@Override
	public int write(char[] chars, int offset, int length) {
		int amountWritten = 0;
		int amountAvailable = 0;
		while ((amountAvailable = Math.min(getWriteAmountAvailable(cycled, readPointer, writePointer), length)) > 0) {
			System.arraycopy(chars, offset, buffer, writePointer, amountAvailable);
			writePointer += amountAvailable;
			if (writePointer >= buffer.length) {
				writePointer = 0;
				cycled = true;
			}
			amountWritten += amountAvailable;
			offset += amountAvailable;
			length -= amountAvailable;
		}
		return amountWritten;
	}

	private int getWriteAmountAvailable(boolean cycled, int readPointer, int writePointer) {
		if (writePointer == 0 && cycled && readPointer == -1)
			return 0;
		else
			return cycled ? readPointer - writePointer : buffer.length - writePointer;
	}
	
	private int getReadAmountAvailable(boolean cycled, int readPointer, int writePointer) {
		if (readPointer == -1)
			return cycled ? buffer.length : writePointer;
		else
			return cycled ? buffer.length - readPointer : writePointer - readPointer;
	}

	@Override
	public int read(char[] chars, int offset, int length) {
		return read(chars, offset, length, true);
	}
	
	private int read(char[] chars, int offset, int length, boolean updateState) {
		int amountRead = 0;
		int amountAvailable = 0;
		int readPointer = this.readPointer;
		boolean cycled = this.cycled;
		while ((amountAvailable = Math.min(getReadAmountAvailable(cycled, readPointer, writePointer), length)) > 0) {
			// if there is an amount available and the readPointer is still set to -1, update it
			if (readPointer == -1)
				readPointer++;
			if (chars != null)
				System.arraycopy(buffer, readPointer, chars, offset, amountAvailable);
			readPointer += amountAvailable;
			if (readPointer >= buffer.length) {
				readPointer = -1;
				cycled = false;
			}
			amountRead += amountAvailable;
			offset += amountAvailable;
			length -= amountAvailable;
		}
		if (updateState) {
			this.cycled = cycled;
			this.readPointer = readPointer;
		}
		return amountRead;
	}

	@Override
	public long remainingData() {
		if (readPointer == -1) {
			return cycled ? buffer.length : writePointer;
		}
		else if (cycled) {
			return buffer.length - readPointer + writePointer;
		}
		else {
			return writePointer - readPointer;
		}
	}

	@Override
	public long remainingSpace() {
		if (writePointer == 0 && cycled && readPointer == -1) {
			return 0;
		}
		else if (cycled) {
			return readPointer - writePointer;
		}
		else {
			return buffer.length - writePointer + (readPointer > 0 ? readPointer : 0);
		}
	}

	@Override
	public void truncate() {
		writePointer = 0;
		readPointer = -1;
		cycled = false;
	}

	@Override
	public long skip(long amount) {
		return amount > 0 ? read(null, 0, (int) amount) : 0;
	}

	@Override
	public long read(CharBuffer target) throws IOException {
		long amountWritten = 0;
		int amountToRead = (int) Math.min(getReadAmountAvailable(cycled, readPointer, writePointer), target.remainingSpace());
		// read to end
		if (amountToRead > 0) {
			amountWritten += amountToRead;
			target.write(buffer, readPointer == -1 ? 0 : readPointer, amountToRead);
			if (readPointer == -1) {
				readPointer = amountToRead;
			}
			else {
				readPointer += amountToRead;
			}
			if (readPointer >= buffer.length) {
				cycled = false;
				readPointer = -1;
				amountToRead = (int) Math.min(getReadAmountAvailable(cycled, readPointer, writePointer), target.remainingSpace());
				if (amountToRead > 0) {
					amountWritten += amountToRead;
					target.write(buffer, 0, amountToRead);
					readPointer = amountToRead;
				}
			}
		}
		return amountWritten;
	}

	@Override
	public long write(CharBuffer buffer) throws IOException {
		return buffer.read(this);
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
	public int read(char[] chars) throws IOException {
		return read(chars, 0, chars.length);
	}

	@Override
	public int write(char[] chars) throws IOException {
		return write(chars, 0, chars.length);
	}

	@Override
	public long peek(CharBuffer target) throws IOException {
		char [] chars = new char[buffer.length];
		int amount = read(chars, 0, (int) Math.min(remainingData(), target.remainingSpace()), false);
		if (target.write(chars, 0, amount) != amount)
			throw new IOException("Could not peek " + amount);
		return amount;
	}

	@Override
	public CharBufferFactory getFactory() {
		return CharBufferFactory.getInstance();
	}
}
