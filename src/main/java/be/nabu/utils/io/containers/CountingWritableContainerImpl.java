package be.nabu.utils.io.containers;

import java.io.IOException;

import be.nabu.utils.io.api.Buffer;
import be.nabu.utils.io.api.CountingWritableContainer;
import be.nabu.utils.io.api.WritableContainer;

public class CountingWritableContainerImpl<T extends Buffer<T>> implements CountingWritableContainer<T> {

	private WritableContainer<T> parent;
	private long writtenTotal = 0;
	
	public CountingWritableContainerImpl(WritableContainer<T> parent) {
		this.parent = parent;
	}
	
	@Override
	public void close() throws IOException {
		parent.close();
	}

	@Override
	public long write(T target) throws IOException {
		long written = parent.write(target);
		if (written > 0)
			writtenTotal += written;
		return written;
	}

	@Override
	public long getWrittenTotal() {
		return writtenTotal;
	}

	@Override
	public void flush() throws IOException {
		parent.flush();
	}
}
