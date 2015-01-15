package be.nabu.utils.io.buffers.bytes;

import java.io.IOException;

import be.nabu.utils.io.api.BufferFactory;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.PeekableContainer;

public class CyclicByteBuffer implements ByteBuffer, PeekableContainer<ByteBuffer> {
	
	private byte [] buffer;
	
	private int writePointer = 0, 
		readPointer = -1;
	
	/**
	 * If the write pointer cycles to the start, it is set to true
	 * If the read pointer cycles to the start, it is set to false
	 */
	private boolean cycled = false;
	
	public CyclicByteBuffer(int size) {
		this.buffer = new byte[size];
	}
	
	public CyclicByteBuffer(byte [] bytes) {
		this.buffer = bytes;
	}
	
	public CyclicByteBuffer(byte [] bytes, int offset, int length) {
		this.buffer = bytes;
		readPointer = offset;
		writePointer = offset + length;
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
	public long read(ByteBuffer target) throws IOException {
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
	public long write(ByteBuffer buffer) throws IOException {
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
	public int read(byte[] bytes) throws IOException {
		return read(bytes, 0, bytes.length);
	}

	@Override
	public int write(byte[] bytes) throws IOException {
		return write(bytes, 0, bytes.length);
	}
	
	@Override
	public long peek(ByteBuffer target) throws IOException {
		byte [] bytes = new byte[buffer.length];
		int amount = read(bytes, 0, (int) Math.min(remainingData(), target.remainingSpace()), false);
		if (target.write(bytes, 0, amount) != amount)
			throw new IOException("Could not peek " + amount);
		return amount;
	}

	@Override
	public BufferFactory<ByteBuffer> getFactory() {
		return ByteBufferFactory.getInstance();
	}

}
