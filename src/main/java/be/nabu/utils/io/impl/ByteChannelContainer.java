package be.nabu.utils.io.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

import be.nabu.utils.io.api.ByteContainer;
import be.nabu.utils.io.api.IORuntimeException;

public class ByteChannelContainer<T extends ByteChannel> implements ByteContainer {
	
	private T channel;

	public ByteChannelContainer(T channel) {
		this.channel = channel;
	}

	public T getChannel() {
		return channel;
	}
	
	@Override
	public int read(byte[] bytes) {
		return read(bytes, 0, bytes.length);
	}

	@Override
	public int read(byte[] bytes, int offset, int length) {
		if (!isReady())
			return 0;
		try {
			return channel.read(ByteBuffer.wrap(bytes, offset, length));
		}
		catch (IOException e) {
			throw new IORuntimeException(e);
		}
	}

	@Override
	public void close() throws IOException {
		channel.close();
	}

	@Override
	public int write(byte[] bytes) {
		return write(bytes, 0, bytes.length);
	}

	@Override
	public int write(byte[] bytes, int offset, int length) {
		if (!isReady())
			return 0;
		try {
			return channel.write(ByteBuffer.wrap(bytes, offset, length));
		}
		catch (IOException e) {
			throw new IORuntimeException(e);
		}
	}

	@Override
	public void flush() {
		// do nothing
	}
	
	protected boolean isReady() {
		return true;
	}
}
