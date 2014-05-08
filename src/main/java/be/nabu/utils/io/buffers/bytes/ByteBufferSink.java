package be.nabu.utils.io.buffers.bytes;

import java.io.IOException;

import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.buffers.BufferSink;

public class ByteBufferSink extends BufferSink<ByteBuffer> implements ByteBuffer {

	public ByteBufferSink(long space) {
		super(ByteBufferFactory.getInstance(), space);
	}

	@Override
	public int read(byte[] bytes, int offset, int length) throws IOException {
		return (int) read(IOUtils.wrap(bytes, offset, length, false));
	}

	@Override
	public int write(byte[] bytes, int offset, int length) throws IOException {
		return (int) write(IOUtils.wrap(bytes, offset, length, true));
	}

	@Override
	public int read(byte[] bytes) throws IOException {
		return read(bytes, 0, bytes.length);
	}

	@Override
	public int write(byte[] bytes) throws IOException {
		return write(bytes, 0, bytes.length);
	}

}
