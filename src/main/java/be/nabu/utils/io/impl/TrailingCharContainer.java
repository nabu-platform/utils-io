package be.nabu.utils.io.impl;

import java.io.IOException;

import be.nabu.utils.io.api.ReadableCharContainer;
import be.nabu.utils.io.impl.CyclicCharContainer;

public class TrailingCharContainer implements ReadableCharContainer {

	private ReadableCharContainer parent;
	private CyclicCharContainer buffer;
	private int trailSize;
	
	public TrailingCharContainer(ReadableCharContainer parent, int trailSize) {
		this.parent = parent;
		this.trailSize = trailSize;
		this.buffer = new CyclicCharContainer(trailSize);
	}

	@Override
	public void close() throws IOException {
		parent.close();
	}

	@Override
	public int read(char[] chars) {
		return read(chars, 0, chars.length);
	}

	@Override
	public int read(char[] chars, int offset, int length) {
		int read = parent.read(chars, offset, length);
		int amountToStore = Math.min(read, trailSize);
		if (amountToStore > 0) {
			if (buffer.remainingSpace() < amountToStore)
				buffer.skip(amountToStore - buffer.remainingSpace());
			buffer.write(chars, offset + (read - amountToStore), amountToStore);
		}
		return read;
	}
	
	public ReadableCharContainer getTrailing() {
		return buffer;
	}
}
