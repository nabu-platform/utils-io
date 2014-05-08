package be.nabu.utils.io.impl;

import java.io.IOException;

import be.nabu.utils.io.api.ReadableByteContainer;
import be.nabu.utils.io.api.SkippableByteContainer;

public class SkippableReadableByteContainerImpl implements SkippableByteContainer {

	private ReadableByteContainer parent;
	private byte [] trash = new byte[10240];
	
	public SkippableReadableByteContainerImpl(ReadableByteContainer parent) {
		this.parent = parent;
	}
	
	@Override
	public int read(byte[] bytes) {
		return parent.read(bytes);
	}

	@Override
	public int read(byte[] bytes, int offset, int length) {
		return parent.read(bytes, offset, length);
	}

	@Override
	public void close() throws IOException {
		parent.close();
	}

	@Override
	public long skip(long amount) {
		long total = 0;
		while (amount > 0) {
			int size = (int) Math.min(amount, trash.length);
			int read = parent.read(trash, 0, size);
			if (read > 0) {
				total += read;
				amount -= read;
			}
			if (read != size)
				break;
		}
		return total;
	}

}
