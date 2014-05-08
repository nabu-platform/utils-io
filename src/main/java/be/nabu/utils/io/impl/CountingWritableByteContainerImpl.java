package be.nabu.utils.io.impl;

import java.io.IOException;

import be.nabu.utils.io.api.CountingWritableByteContainer;
import be.nabu.utils.io.api.WritableByteContainer;

public class CountingWritableByteContainerImpl implements CountingWritableByteContainer {

	private WritableByteContainer parent;
	private long writtenTotal = 0;
	
	public CountingWritableByteContainerImpl(WritableByteContainer parent) {
		this.parent = parent;
	}
	
	@Override
	public void close() throws IOException {
		parent.close();
	}

	@Override
	public int write(byte[] bytes) {
		int written = parent.write(bytes);
		if (written > 0)
			writtenTotal += written;
		return written;
	}

	@Override
	public int write(byte[] bytes, int offset, int length) {
		int written = parent.write(bytes, offset, length);
		if (written > 0)
			writtenTotal += written;
		return written;
	}
	
	public long getWrittenTotal() {
		return writtenTotal;
	}

	public void resetWrittenTotal() {
		writtenTotal = 0;
	}

	@Override
	public void flush() {
		parent.flush();
	}
}
