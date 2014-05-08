package be.nabu.utils.io.impl;

import java.io.IOException;

import be.nabu.utils.io.api.CountingWritableCharContainer;
import be.nabu.utils.io.api.WritableCharContainer;

public class CountingWritableCharContainerImpl implements CountingWritableCharContainer {

	private WritableCharContainer parent;
	private long writtenTotal = 0;
	
	public CountingWritableCharContainerImpl(WritableCharContainer parent) {
		this.parent = parent;
	}
	
	@Override
	public void close() throws IOException {
		parent.close();
	}

	@Override
	public int write(char[] chars) {
		int written = parent.write(chars);
		if (written > 0)
			writtenTotal += written;
		return written;
	}

	@Override
	public int write(char[] chars, int offset, int length) {
		int written = parent.write(chars, offset, length);
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
