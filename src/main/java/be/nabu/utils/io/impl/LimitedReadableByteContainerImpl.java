package be.nabu.utils.io.impl;

import java.io.IOException;

import be.nabu.utils.io.api.LimitedReadableByteContainer;
import be.nabu.utils.io.api.ReadableByteContainer;

public class LimitedReadableByteContainerImpl implements LimitedReadableByteContainer {

	private ReadableByteContainer parent;
	private long limit;
	private long readTotal;
	
	public <T extends ReadableByteContainer & LimitedReadableByteContainer> LimitedReadableByteContainerImpl(T parent) {
		this.parent = parent;
		limit = parent.remainingData();
	}
	
	public LimitedReadableByteContainerImpl(ReadableByteContainer parent, long limit) {
		this.parent = parent;
		this.limit = limit;
	}

	@Override
	public void close() throws IOException {
		parent.close();
	}

	@Override
	public long remainingData() {
		return limit - readTotal;
	}

	@Override
	public int read(byte[] bytes) {
		return read(bytes, 0, bytes.length);
	}

	@Override
	public int read(byte[] bytes, int offset, int length) {
		int read = parent.read(bytes, offset, (int) Math.min(length, limit - readTotal));
		if (read > 0)
			readTotal += read;
		return read == 0 && readTotal == limit ? -1 : read;
	}

}
