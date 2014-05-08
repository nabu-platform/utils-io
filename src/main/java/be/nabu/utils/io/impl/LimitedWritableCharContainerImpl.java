package be.nabu.utils.io.impl;

import java.io.IOException;

import be.nabu.utils.io.api.LimitedWritableCharContainer;
import be.nabu.utils.io.api.WritableCharContainer;

public class LimitedWritableCharContainerImpl implements LimitedWritableCharContainer {

	private WritableCharContainer parent;
	private long limit;
	private long writtenTotal;
		
	public LimitedWritableCharContainerImpl(WritableCharContainer parent, long limit) {
		this.parent = parent;
		this.limit = limit;
	}

	@Override
	public int write(char[] chars) {
		return write(chars, 0, chars.length);
	}

	@Override
	public int write(char[] chars, int offset, int length) {
		length = (int) Math.min(length, remainingSpace());
		int written = parent.write(chars, offset, length);
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
