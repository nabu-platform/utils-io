package be.nabu.utils.io.impl;

import java.io.IOException;

import be.nabu.utils.io.api.ReadableByteContainer;

public class BlockingReadableByteContainer implements ReadableByteContainer {

	private ReadableByteContainer parent;
	
	public BlockingReadableByteContainer(ReadableByteContainer parent) {
		this.parent = parent;
	}
	
	@Override
	public void close() throws IOException {
		parent.close();
	}

	@Override
	public int read(byte[] bytes) {
		return read(bytes, 0, bytes.length);
	}

	@Override
	public int read(byte[] bytes, int offset, int length) {
		int read = 0;
		while (read == 0)
			read = parent.read(bytes, offset, length);
		return read;
	}

}
