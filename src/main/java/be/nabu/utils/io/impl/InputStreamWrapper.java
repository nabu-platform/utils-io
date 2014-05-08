package be.nabu.utils.io.impl;

import java.io.IOException;
import java.io.InputStream;

import be.nabu.utils.io.api.IORuntimeException;
import be.nabu.utils.io.api.ReadableByteContainer;

public class InputStreamWrapper implements ReadableByteContainer {

	private InputStream input;
	
	public InputStreamWrapper(InputStream input) {
		this.input = input;
	}
	
	@Override
	public void close() throws IOException {
		input.close();
	}

	@Override
	public int read(byte [] bytes) {
		return read(bytes, 0, bytes.length);
	}

	@Override
	public int read(byte[] bytes, int offset, int length) {
		try {
			return input.read(bytes, offset, length);
		}
		catch (IOException e) {
			throw new IORuntimeException(e);
		}
	}

}
