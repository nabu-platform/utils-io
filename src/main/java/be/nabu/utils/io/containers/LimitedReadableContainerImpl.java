package be.nabu.utils.io.containers;

import java.io.IOException;

import be.nabu.utils.io.api.Buffer;
import be.nabu.utils.io.api.LimitedReadableContainer;
import be.nabu.utils.io.api.ReadableContainer;

public class LimitedReadableContainerImpl<T extends Buffer<T>> implements LimitedReadableContainer<T> {

	private ReadableContainer<T> parent;
	private long limit;
	private long readTotal;
	
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
		long read = parent.read(target.getFactory().limit(target, null, remainingData()));
		if (read > 0) {
			readTotal += read;
		}
		return read == 0 && readTotal == limit ? -1 : read;
	}
}
