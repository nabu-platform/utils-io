package be.nabu.utils.io.impl;

import java.io.IOException;

import be.nabu.utils.io.api.WritableByteContainer;

public class BlockingWritableByteContainer implements WritableByteContainer {

	private WritableByteContainer parent;
	
	public BlockingWritableByteContainer(WritableByteContainer parent) {
		this.parent = parent;
	}
	
	@Override
	public void close() throws IOException {
		parent.close();
	}

	@Override
	public int write(byte[] bytes) {
		return write(bytes, 0, bytes.length);
	}

	@Override
	public int write(byte[] bytes, int offset, int length) {
		int written = 0;
		while (written < length)
			written += parent.write(bytes, offset + written, length - written);
		return written;
	}

	@Override
	public void flush() {
		parent.flush();
	}

}
