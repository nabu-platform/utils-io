package be.nabu.utils.io.impl;

import java.io.IOException;

import be.nabu.utils.io.api.WritableByteContainer;

public class ByteContainerSink implements WritableByteContainer {

	@Override
	public void close() throws IOException {
		// do nothing
	}

	@Override
	public int write(byte[] bytes) {
		return write(bytes, 0, bytes.length);
	}

	@Override
	public int write(byte[] bytes, int offset, int length) {
		// consider your data well and truly lost!
		return length;
	}

	@Override
	public void flush() {
		// tempting but no
	}

}
