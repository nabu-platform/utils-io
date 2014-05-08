package be.nabu.utils.io.impl;

import java.io.IOException;

import be.nabu.utils.io.api.LimitedWritableByteContainer;
import be.nabu.utils.io.api.WritableByteContainer;

public class LimitedWritableByteContainerImpl implements LimitedWritableByteContainer {

	private WritableByteContainer parent;
	private long limit;
	private long writtenTotal;
		
	public LimitedWritableByteContainerImpl(WritableByteContainer parent, long limit) {
		this.parent = parent;
		this.limit = limit;
	}

	@Override
	public int write(byte[] bytes) {
		return write(bytes, 0, bytes.length);
	}

	@Override
	public int write(byte[] bytes, int offset, int length) {
		length = (int) Math.min(length, remainingSpace());
		int written = parent.write(bytes, offset, length);
		writtenTotal += written;
		return written;
	}

	@Override
	public void close() throws IOException {
		parent.close();
	}

	@Override
	public long remainingSpace() {
		return limit - writtenTotal;
	}

	@Override
	public void flush() {
		parent.flush();
	}

}
