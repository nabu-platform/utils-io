package be.nabu.utils.io.impl;

import java.io.IOException;

import be.nabu.utils.io.api.LimitedReadableCharContainer;
import be.nabu.utils.io.api.ReadableCharContainer;

public class LimitedReadableCharContainerImpl implements LimitedReadableCharContainer {

	private ReadableCharContainer parent;
	private long limit;
	private long readTotal;
	
	public <T extends ReadableCharContainer & LimitedReadableCharContainer> LimitedReadableCharContainerImpl(T parent) {
		this.parent = parent;
		limit = parent.remainingData();
	}
	
	public LimitedReadableCharContainerImpl(ReadableCharContainer parent, long limit) {
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
	public int read(char[] chars) {
		return read(chars, 0, chars.length);
	}

	@Override
	public int read(char[] chars, int offset, int length) {
		int read = parent.read(chars, offset, (int) Math.min(length, limit - readTotal));
		if (read > 0)
			readTotal += read;
		return read == 0 && readTotal == limit ? -1 : read;
	}

}
