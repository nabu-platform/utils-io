package be.nabu.utils.io.buffers.chars;

import be.nabu.utils.io.api.BufferFactory;
import be.nabu.utils.io.api.CharBuffer;
import be.nabu.utils.io.buffers.LimitedCharBuffer;

public class CharBufferFactory implements BufferFactory<CharBuffer> {

	private static CharBufferFactory instance;
	
	public static CharBufferFactory getInstance() {
		if (instance == null)
			instance = new CharBufferFactory();
		return instance;
	}
	
	private CharBufferFactory() {
		// hide
	}
	
	@Override
	public CharBuffer newInstance(long size, boolean cyclic) {
		return cyclic ? new CyclicCharBuffer((int) size) : new StaticCharBuffer((int) size);
	}

	@Override
	public CharBuffer newInstance() {
		return new DynamicCharBuffer();
	}

	@Override
	public CharBuffer newSink(long size) {
		return new CharBufferSink(size);
	}

	@Override
	public CharBuffer limit(CharBuffer buffer, Long maxRead, Long maxWrite) {
		return new LimitedCharBuffer(buffer, maxRead, maxWrite);
	}
}
