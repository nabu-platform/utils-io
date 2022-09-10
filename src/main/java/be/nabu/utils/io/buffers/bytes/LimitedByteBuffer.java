package be.nabu.utils.io.buffers.bytes;

import java.io.IOException;

import be.nabu.utils.io.api.BufferFactory;
import be.nabu.utils.io.api.ByteBuffer;

/**
 * This is a buffer that wraps around the incoming buffer
 * It simply limits the announced space remaining in this buffer for reading, that way no one should write too much to it
 */
public class LimitedByteBuffer implements ByteBuffer {

	private ByteBuffer original;
	private long maxWrite;
	private long maxRead;

	public LimitedByteBuffer(ByteBuffer original, Long maxRead, Long maxWrite) {
		this.original = original;
		this.maxRead = maxRead == null ? Long.MAX_VALUE : maxRead;
		this.maxWrite = maxWrite == null ? Long.MAX_VALUE : maxWrite;
	}

	@Override
	public void close() throws IOException {
		original.close();
	}

	@Override
	public void flush() throws IOException {
		original.flush();
	}

	@Override
	public long remainingData() {
		return Math.min(original.remainingData(), maxRead);
	}

	@Override
	public long remainingSpace() {
		return Math.min(original.remainingSpace(), maxWrite);
	}

	@Override
	public void truncate() {
		original.truncate();
	}

	@Override
	public long skip(long amount) throws IOException {
		amount = original.skip(Math.min(amount, remainingData()));
		long skip = original.skip(amount);
		maxRead -= skip;
		return skip;
	}

	@Override
	public long read(ByteBuffer buffer) throws IOException {
		long remainingData = remainingData();
		if (buffer.remainingSpace() > remainingData) {
			buffer = new LimitedByteBuffer(buffer, null, remainingData);
		}
		long read = original.read(buffer);
		maxRead -= read;
		return read;
	}

	@Override
	public long write(ByteBuffer buffer) throws IOException {
		long remainingSpace = remainingSpace();
		if (buffer.remainingData() > remainingSpace) {
			buffer = new LimitedByteBuffer(buffer, remainingSpace, null);
		}
		long write = original.write(buffer);
		maxWrite -= write;
		return write;
	}

	@Override
	public long peek(ByteBuffer buffer) throws IOException {
		long remainingData = remainingData();
		if (buffer.remainingSpace() > remainingData) {
			buffer = new LimitedByteBuffer(buffer, null, remainingData);
		}
		return original.peek(buffer);
	}

	@Override
	public int read(byte[] bytes, int offset, int length) throws IOException {
		length = (int) Math.min(length, remainingData());
		int read = original.read(bytes, offset, length);
		maxRead -= read;
		return read;
	}

	@Override
	public int write(byte[] bytes, int offset, int length) throws IOException {
		length = (int) Math.min(length, remainingSpace());
		int write = original.write(bytes, offset, length);
		maxWrite -= write;
		return write;
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
		return original.getFactory();
	}
}