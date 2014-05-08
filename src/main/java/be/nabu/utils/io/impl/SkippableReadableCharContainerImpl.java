package be.nabu.utils.io.impl;

import java.io.IOException;

import be.nabu.utils.io.api.ReadableCharContainer;
import be.nabu.utils.io.api.SkippableCharContainer;

public class SkippableReadableCharContainerImpl implements SkippableCharContainer {

	private ReadableCharContainer parent;
	private char [] trash = new char[10240];
	
	public SkippableReadableCharContainerImpl(ReadableCharContainer parent) {
		this.parent = parent;
	}
	
	@Override
	public int read(char[] chars) {
		return parent.read(chars);
	}

	@Override
	public int read(char[] chars, int offset, int length) {
		return parent.read(chars, offset, length);
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
