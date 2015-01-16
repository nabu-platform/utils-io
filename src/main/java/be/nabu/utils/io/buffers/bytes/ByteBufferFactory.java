package be.nabu.utils.io.buffers.bytes;

import be.nabu.utils.io.api.BufferFactory;
import be.nabu.utils.io.api.ByteBuffer;

public class ByteBufferFactory implements BufferFactory<ByteBuffer> {

	private static ByteBufferFactory instance;
	
	public static ByteBufferFactory getInstance() {
		if (instance == null)
			instance = new ByteBufferFactory();
		return instance;
	}
	
	private ByteBufferFactory() {
		// hide
	}
	
	@Override
	public ByteBuffer newInstance(long size, boolean cyclic) {
		return cyclic ? new CyclicByteBuffer((int) size) : new StaticByteBuffer((int) size);
	}

	@Override
	public ByteBuffer newInstance() {
		return new DynamicByteBuffer();
	}

	@Override
	public ByteBuffer newSink(long size) {
		return new ByteBufferSink(size);
	}

	@Override
	public ByteBuffer limit(ByteBuffer buffer, Long maxRead, Long maxWrite) {
		return new LimitedByteBuffer(buffer, maxRead, maxWrite);
	}
}
