package be.nabu.utils.io.impl;

import java.io.IOException;

import be.nabu.utils.io.api.PushbackByteContainer;
import be.nabu.utils.io.api.ReadableByteContainer;

public class PushbackByteContainerImpl implements PushbackByteContainer {

	private ReadableByteContainer container;
	
	private DynamicByteContainer buffer = new DynamicByteContainer();
	
	public PushbackByteContainerImpl(ReadableByteContainer container) {
		this.container = container;
	}
	
	@Override
	public int read(byte[] bytes) {
		return read(bytes, 0, bytes.length);
	}

	@Override
	public int read(byte[] bytes, int offset, int length) {
		int read = 0;
		if (buffer.remainingData() > 0)
			read += buffer.read(bytes, offset, (int) Math.min(length, buffer.remainingData()));
		if (read < length) {
			int tmpRead = container.read(bytes, offset + read, length - read);
			if (tmpRead == -1)
				return read == 0 ? -1 : read;
			else
				read += tmpRead;
		}
		return read;
	}

	@Override
	public void close() throws IOException {
		container.close();
	}

	@Override
	public void pushback(byte[] bytes) {
		buffer.write(bytes);
	}

	@Override
	public void pushback(byte[] bytes, int offset, int length) {
		buffer.write(bytes, offset, length);
	}

	public long getBufferSize() {
		return buffer.remainingData();
	}
}
