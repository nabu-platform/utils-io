package be.nabu.utils.io.impl;

import java.io.IOException;
import java.io.OutputStream;

import be.nabu.utils.io.api.IORuntimeException;
import be.nabu.utils.io.api.WritableByteContainer;

public class OutputStreamWrapper implements WritableByteContainer {

	private OutputStream output;
	
	public OutputStreamWrapper(OutputStream output) {
		this.output = output;
	}
	
	@Override
	public void close() throws IOException {
		output.close();
	}

	@Override
	public int write(byte[] bytes) {
		return write(bytes, 0, bytes.length);
	}

	@Override
	public int write(byte[] bytes, int offset, int length) {
		try {
			output.write(bytes, offset, length);
			return length;
		}
		catch (IOException e) {
			throw new IORuntimeException(e);
		}
	}

	@Override
	public void flush() {
		try {
			output.flush();
		}
		catch (IOException e) {
			throw new IORuntimeException(e);
		}
	}

}
