package be.nabu.utils.io.impl;

import java.io.IOException;

import be.nabu.utils.io.api.WritableCharContainer;

public class CharContainerSink implements WritableCharContainer {

	@Override
	public void close() throws IOException {
		// do nothing
	}

	@Override
	public int write(char[] chars) {
		return write(chars, 0, chars.length);
	}

	@Override
	public int write(char[] chars, int offset, int length) {
		return length;
	}

	@Override
	public void flush() {
		// tempting but no
	}

}
