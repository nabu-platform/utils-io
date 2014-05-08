package be.nabu.utils.io.impl;

import java.io.IOException;

import be.nabu.utils.io.api.CharContainer;
import be.nabu.utils.io.api.ReadableCharContainer;
import be.nabu.utils.io.api.WritableCharContainer;

public class ComposedCharContainer implements CharContainer {

	private ReadableCharContainer readable;
	private WritableCharContainer writable;
	
	public ComposedCharContainer(ReadableCharContainer readable, WritableCharContainer writable) {
		this.readable = readable;
		this.writable = writable;
	}

	@Override
	public int read(char[] chars) {
		return readable.read(chars);
	}

	@Override
	public int read(char[] chars, int offset, int length) {
		return readable.read(chars, offset, length);
	}

	@Override
	public void close() throws IOException {
		try {
			writable.close();
		}
		finally {
			readable.close();
		}
	}

	@Override
	public int write(char[] chars) {
		return writable.write(chars);
	}

	@Override
	public int write(char[] chars, int offset, int length) {
		return writable.write(chars, offset, length);
	}

	@Override
	public void flush() {
		writable.flush();
	}

}
