package be.nabu.utils.io.impl;

import java.io.IOException;

import be.nabu.utils.io.api.ByteContainer;
import be.nabu.utils.io.api.ReadableByteContainer;
import be.nabu.utils.io.api.WritableByteContainer;

public class ComposedByteContainer implements ByteContainer {

	private ReadableByteContainer readable;
	private WritableByteContainer writable;
	
	public ComposedByteContainer(ReadableByteContainer readable, WritableByteContainer writable) {
		this.readable = readable;
		this.writable = writable;
	}

	@Override
	public int read(byte[] bytes) {
		return readable.read(bytes);
	}

	@Override
	public int read(byte[] bytes, int offset, int length) {
		return readable.read(bytes, offset, length);
	}

	@Override
	public void close() throws IOException {
		// close writable first as this may trigger a flush() which writes some more
		try {
			writable.close();
		}
		finally {
			readable.close();
		}
	}

	@Override
	public int write(byte[] bytes) {
		return writable.write(bytes);
	}

	@Override
	public int write(byte[] bytes, int offset, int length) {
		return writable.write(bytes, offset, length);
	}

	@Override
	public void flush() {
		writable.flush();
	}

}
