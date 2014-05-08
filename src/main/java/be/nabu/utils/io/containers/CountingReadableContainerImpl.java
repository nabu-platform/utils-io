package be.nabu.utils.io.containers;

import java.io.IOException;

import be.nabu.utils.io.api.Buffer;
import be.nabu.utils.io.api.CountingReadableContainer;
import be.nabu.utils.io.api.ReadableContainer;

public class CountingReadableContainerImpl<T extends Buffer<T>> implements CountingReadableContainer<T> {

	private ReadableContainer<T> parent;
	private long readTotal = 0;
	
	public CountingReadableContainerImpl(ReadableContainer<T> parent) {
		this.parent = parent;
	}
	
	@Override
	public void close() throws IOException {
		parent.close();
	}

	@Override
	public long read(T target) throws IOException {
		long read = parent.read(target);
		if (read > 0)
			readTotal += read;
		return read;
	}
	
	@Override
	public long getReadTotal() {
		return readTotal;
	}

}
