package be.nabu.utils.io.impl;

import java.io.IOException;
import java.io.Writer;

import be.nabu.utils.io.api.WritableCharContainer;

public class WritableCharContainerWriter extends Writer {

	private WritableCharContainer container;
	
	public WritableCharContainerWriter(WritableCharContainer container) {
		this.container = container;
	}
	
	@Override
	public void close() throws IOException {
		container.close();
	}

	@Override
	public void flush() throws IOException {
		container.flush();
	}

	@Override
	public void write(char[] characters, int offset, int length) throws IOException {
		container.write(characters, offset, length);
	}

}
