package be.nabu.utils.io.impl;

import java.io.IOException;

import be.nabu.utils.io.api.PushbackCharContainer;
import be.nabu.utils.io.api.ReadableCharContainer;

public class PushbackCharContainerImpl implements PushbackCharContainer {

	private ReadableCharContainer container;
	
	private DynamicCharContainer buffer = new DynamicCharContainer();
	
	public PushbackCharContainerImpl(ReadableCharContainer container) {
		this.container = container;
	}
	
	@Override
	public int read(char[] chars) {
		return read(chars, 0, chars.length);
	}

	@Override
	public int read(char[] chars, int offset, int length) {
		int read = 0;
		if (buffer.remainingData() > 0)
			read = buffer.read(chars, offset, (int) Math.min(length, buffer.remainingData()));
		if (read < length)
			read += container.read(chars, offset + read, length - read);
		return read;
	}

	@Override
	public void close() throws IOException {
		container.close();
	}

	@Override
	public void pushback(char[] chars) {
		buffer.write(chars);
	}

	@Override
	public void pushback(char[] chars, int offset, int length) {
		buffer.write(chars, offset, length);
	}

}
