package be.nabu.utils.io.impl;

import java.io.IOException;

import be.nabu.utils.io.api.ReadableCharContainer;

public class EOFReadableCharContainer implements ReadableCharContainer {

	private ReadableCharContainer parent;
	private boolean eof = false;
	
	public EOFReadableCharContainer(ReadableCharContainer parent) {
		this.parent = parent;
	}
	
	@Override
	public void close() throws IOException {
		parent.close();
		eof = true;
	}

	@Override
	public int read(char[] chars) {
		int read = parent.read(chars);
		if (read == -1)
			eof = true;
		return read;
	}

	@Override
	public int read(char[] chars, int offset, int length) {
		int read = parent.read(chars, offset, length);
		if (read == -1)
			eof = true;
		return read;
	}
	
	public boolean isEOF() {
		return eof;
	}
}
