package be.nabu.utils.io.impl;

import java.io.IOException;

import be.nabu.utils.io.api.ReadableByteContainer;

public class EOFReadableByteContainer implements ReadableByteContainer {

	private ReadableByteContainer parent;
	private boolean eof = false;
	
	public EOFReadableByteContainer(ReadableByteContainer parent) {
		this.parent = parent;
	}
	
	@Override
	public void close() throws IOException {
		parent.close();
		eof = true;
	}

	@Override
	public int read(byte[] bytes) {
		int read = parent.read(bytes);
		if (read == -1)
			eof = true;
		return read;
	}

	@Override
	public int read(byte[] bytes, int offset, int length) {
		int read = parent.read(bytes, offset, length);
		if (read == -1)
			eof = true;
		return read;
	}
	
	public boolean isEOF() {
		return eof;
	}
}
