package be.nabu.utils.io.impl;

import java.io.IOException;
import java.io.OutputStream;

import be.nabu.utils.io.api.WritableByteContainer;

public class WritableByteContainerOutputStream extends OutputStream {

	private WritableByteContainer container;
	private byte [] buffer = new byte [1];
	
	public WritableByteContainerOutputStream(WritableByteContainer container) {
		this.container = container;
	}
	
	@Override
	public void write(int arg0) throws IOException {
		buffer[0] = (byte) arg0;
		write(buffer);
	}

	@Override
	public void close() throws IOException {
		container.close();
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		container.write(b, off, len);
	}

	@Override
	public void write(byte[] b) throws IOException {
		container.write(b);
	}
	
}
