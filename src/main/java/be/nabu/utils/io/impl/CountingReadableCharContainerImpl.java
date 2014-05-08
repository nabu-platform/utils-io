package be.nabu.utils.io.impl;

import java.io.IOException;

import be.nabu.utils.io.api.CountingReadableCharContainer;
import be.nabu.utils.io.api.ReadableCharContainer;

public class CountingReadableCharContainerImpl implements CountingReadableCharContainer {

	private ReadableCharContainer parent;
	private long readTotal = 0;
	
	public CountingReadableCharContainerImpl(ReadableCharContainer parent) {
		this.parent = parent;
	}
	
	@Override
	public void close() throws IOException {
		parent.close();
	}

	@Override
	public int read(char[] chars) {
		int read = parent.read(chars);
		if (read > 0)
			readTotal += read;
		return read;
	}

	@Override
	public int read(char[] chars, int offset, int length) {
		int read = parent.read(chars, offset, length);
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
