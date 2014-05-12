package be.nabu.utils.io.containers;

import java.io.IOException;

import be.nabu.utils.io.api.Buffer;
import be.nabu.utils.io.api.LimitedReadableContainer;
import be.nabu.utils.io.api.ReadableContainer;

public class LimitedReadableContainerImpl<T extends Buffer<T>> implements LimitedReadableContainer<T> {

	private ReadableContainer<T> parent;
	private long limit;
	private long readTotal;
	private T limitedBuffer;
	
	public <S extends ReadableContainer<T> & LimitedReadableContainer<T>> LimitedReadableContainerImpl(S parent) {
		this.parent = parent;
		limit = parent.remainingData();
	}
	
	public LimitedReadableContainerImpl(ReadableContainer<T> parent, long limit) {
		this.parent = parent;
		this.limit = limit;
	}

	@Override
	public void close() throws IOException {
		parent.close();
	}

	@Override
	public long remainingData() {
		return limit - readTotal;
	}

	@Override
	public long read(T target) throws IOException {
		// this is an optimization for large reads that assumes that you reuse a buffer with a fixed size
		// instead of creating new buffers for each read, it will truncate the existing one in the hopes that it matches the requested read buffer
		// when reading 100 meg using a 4096 byte buffer, you can reuse it approximately 25600 times
		if (limitedBuffer != null)
			limitedBuffer.truncate();
		
		if (limitedBuffer == null || limitedBuffer.remainingSpace() < target.remainingSpace() || remainingData() < target.remainingSpace())
			limitedBuffer = target.getFactory().newInstance(Math.min(remainingData(), target.remainingSpace()), false);
		
		long read = parent.read(limitedBuffer);
		if (read > 0) {
			readTotal += read;
			target.write(limitedBuffer);
		}
		return read == 0 && readTotal == limit ? -1 : read;
	}

}
