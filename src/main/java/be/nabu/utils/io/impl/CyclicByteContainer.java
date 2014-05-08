package be.nabu.utils.io.impl;

import be.nabu.utils.io.api.ByteContainer;
import be.nabu.utils.io.api.LimitedReadableByteContainer;
import be.nabu.utils.io.api.LimitedWritableByteContainer;
import be.nabu.utils.io.api.PeekableByteContainer;
import be.nabu.utils.io.api.SkippableByteContainer;
import be.nabu.utils.io.api.TruncatableByteContainer;

public class CyclicByteContainer implements ByteContainer, PeekableByteContainer, LimitedWritableByteContainer, LimitedReadableByteContainer, TruncatableByteContainer, SkippableByteContainer {
	
	private byte [] buffer;
	
	private int writePointer = 0, 
		readPointer = -1;
	
	/**
	 * If the write pointer cycles to the start, it is set to true
	 * If the read pointer cycles to the start, it is set to false
	 */
	private boolean cycled = false;
	
	public CyclicByteContainer(int size) {
		this.buffer = new byte[size];
	}

	@Override
	public int write(byte[] bytes) {
		return write(bytes, 0, bytes.length);
	}

	@Override
	public int write(byte[] bytes, int offset, int length) {
		int amountWritten = 0;
		int amountAvailable = 0;
		while ((amountAvailable = Math.min(getWriteAmountAvailable(cycled, readPointer, writePointer), length)) > 0) {
			System.arraycopy(bytes, offset, buffer, writePointer, amountAvailable);
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
	public int read(byte[] bytes) {
		return read(bytes, 0, bytes.length, true);
	}

	@Override
	public int read(byte[] bytes, int offset, int length) {
		return read(bytes, offset, length, true);
	}
	
	private int read(byte[] bytes, int offset, int length, boolean updateState) {
		int amountRead = 0;
		int amountAvailable = 0;
		int readPointer = this.readPointer;
		boolean cycled = this.cycled;
		while ((amountAvailable = Math.min(getReadAmountAvailable(cycled, readPointer, writePointer), length)) > 0) {
			// if there is an amount available and the readPointer is still set to -1, update it
			if (readPointer == -1)
				readPointer++;
			if (bytes != null)
				System.arraycopy(buffer, readPointer, bytes, offset, amountAvailable);
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
	public void flush() {
		// do nothing
	}

	@Override
	public void close() {
		// do nothing
	}

	@Override
	public int peak(byte[] bytes) {
		return peak(bytes, 0, bytes.length);
	}

	@Override
	public int peak(byte[] bytes, int offset, int length) {
		return read(bytes, offset, length, false);
	}

	@Override
	public long remainingData() {
		int remainingData = 0;
		int readPointer = this.readPointer;
		boolean cycled = this.cycled;
		int amountAvailable = 0;
		while ((amountAvailable = getReadAmountAvailable(cycled, readPointer, writePointer)) > 0) {
			remainingData += amountAvailable;
			if (readPointer == -1)
				readPointer++;
			readPointer += amountAvailable;
			if (readPointer >= buffer.length) {
				readPointer = -1;
				cycled = false;
			}
		}
		return remainingData;
	}

	@Override
	public long remainingSpace() {
		int remainingSpace = 0;
		int writePointer = this.writePointer;
		boolean cycled = this.cycled;
		int amountAvailable = 0;
		while ((amountAvailable = getWriteAmountAvailable(cycled, readPointer, writePointer)) > 0) {
			remainingSpace += amountAvailable;
			writePointer += amountAvailable;
			if (writePointer >= buffer.length) {
				writePointer = 0;
				cycled = true;
			}
		}
		return remainingSpace;
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
}
