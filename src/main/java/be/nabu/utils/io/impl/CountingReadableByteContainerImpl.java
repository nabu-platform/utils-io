package be.nabu.utils.io.impl;

import java.io.IOException;

import be.nabu.utils.io.api.CountingReadableByteContainer;
import be.nabu.utils.io.api.ReadableByteContainer;

public class CountingReadableByteContainerImpl implements CountingReadableByteContainer {

	private ReadableByteContainer parent;
	private long readTotal = 0;
	
	public CountingReadableByteContainerImpl(ReadableByteContainer parent) {
		this.parent = parent;
	}
	
	@Override
	public void close() throws IOException {
		parent.close();
	}

	@Override
	public int read(byte[] bytes) {
		int read = parent.read(bytes);
		if (read > 0)
			readTotal += read;
		return read;
	}

	@Override
	public int read(byte[] bytes, int offset, int length) {
		int read = parent.read(bytes, offset, length);
		if (read > 0)
			readTotal += read;
		return read;
	}
	
	@Override
	public long getReadTotal() {
		return readTotal;
	}

	@Override
	public void resetReadTotal() {
		readTotal = 0;
	}

}
