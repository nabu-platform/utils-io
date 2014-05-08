package be.nabu.utils.io.buffers;

import java.io.IOException;

import be.nabu.utils.io.api.Buffer;
import be.nabu.utils.io.api.BufferFactory;

public class BufferSink<T extends Buffer<T>> implements Buffer<T> {

	private long space;
	private BufferFactory<T> factory;
	
	public BufferSink(BufferFactory<T> factory, long space) {
		this.factory = factory;
		this.space = space;
	}
	
	@Override
	public long read(T buffer) throws IOException {
		return 0;
	}

	@Override
	public void close() throws IOException {
		// do nothing
	}

	@Override
	public long write(T buffer) throws IOException {
		long amount = 0;
		if (space < 0)
			amount = buffer.remainingData();
		else {
			amount = Math.min(buffer.remainingData(), space);
			space -= amount;
		}
		return buffer.skip(amount);
	}

	@Override
	public void flush() throws IOException {
		// do nothing
	}

	@Override
	public long remainingData() {
		return 0;
	}

	@Override
	public long remainingSpace() {
		return space == -1 ? Long.MAX_VALUE : space;
	}

	@Override
	public void truncate() {
		// do nothing
	}

	@Override
	public long skip(long amount) {
		return 0;
	}

	@Override
	public BufferFactory<T> getFactory() {
		return factory;
	}

	@Override
	public long peek(T buffer) throws IOException {
		return 0;
	}

}
