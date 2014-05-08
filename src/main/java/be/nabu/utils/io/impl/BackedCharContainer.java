package be.nabu.utils.io.impl;

import java.io.IOException;
import java.nio.charset.Charset;

import be.nabu.utils.io.api.ByteContainer;
import be.nabu.utils.io.api.CharContainer;

public class BackedCharContainer implements CharContainer {

	private BackedReadableCharContainer readable;
	private BackedWritableCharContainer writable;
	
	public BackedCharContainer(ByteContainer container, Charset charset) {
		readable = new BackedReadableCharContainer(container, charset);
		writable = new BackedWritableCharContainer(container, charset);
	}
	
	@Override
	public int read(char[] chars) {
		return read(chars, 0, chars.length);
	}

	@Override
	public int read(char[] chars, int offset, int length) {
		return readable.read(chars, offset, length);
	}

	@Override
	public void close() throws IOException {
		// close writable first so it can flush the data
		// if the readable is the same object as the writable and you close readable first, then flush before closing writable
		// you get an exception
		try {
			writable.close();
		}
		finally {
			readable.close();
		}
	}

	@Override
	public int write(char[] chars) {
		return write(chars, 0, chars.length);
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
