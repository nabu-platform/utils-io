package be.nabu.utils.io.impl;

import java.io.IOException;
import java.io.Writer;

import be.nabu.utils.io.api.IORuntimeException;
import be.nabu.utils.io.api.WritableCharContainer;

public class WriterWrapper implements WritableCharContainer {

	private Writer writer;
	
	public WriterWrapper(Writer writer) {
		this.writer = writer;
	}
	
	@Override
	public void close() throws IOException {
		writer.close();
	}

	@Override
	public int write(char[] chars) {
		return write(chars, 0, chars.length);
	}

	@Override
	public int write(char[] chars, int offset, int length) {
		try {
			writer.write(chars, offset, length);
			return length;
		}
		catch (IOException e) {
			throw new IORuntimeException(e);
		}
	}

	@Override
	public void flush() {
		try {
			writer.flush();
		}
		catch (IOException e) {
			throw new IORuntimeException(e);
		}
	}

}
