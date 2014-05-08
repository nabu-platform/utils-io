package be.nabu.utils.io.impl;

import java.io.IOException;
import java.io.Reader;

import be.nabu.utils.io.api.IORuntimeException;
import be.nabu.utils.io.api.ReadableCharContainer;

public class ReaderWrapper implements ReadableCharContainer {

	private Reader reader;
	
	public ReaderWrapper(Reader reader) {
		this.reader = reader;
	}
	
	@Override
	public void close() throws IOException {
		reader.close();
	}

	@Override
	public int read(char[] chars) {
		return read(chars, 0, chars.length);
	}

	@Override
	public int read(char[] chars, int offset, int length) {
		try {
			return reader.read(chars, offset, length);
		}
		catch (IOException e) {
			throw new IORuntimeException(e);
		}
	}

}
