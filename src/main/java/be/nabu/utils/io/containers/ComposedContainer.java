package be.nabu.utils.io.containers;

import java.io.IOException;

import be.nabu.utils.io.api.Buffer;
import be.nabu.utils.io.api.Container;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.api.WritableContainer;

public class ComposedContainer<T extends Buffer<T>> implements Container<T> {

	private ReadableContainer<T> readable;
	private WritableContainer<T> writable;
	
	public ComposedContainer(ReadableContainer<T> readable, WritableContainer<T> writable) {
		this.readable = readable;
		this.writable = writable;
	}

	@Override
	public long read(T buffer) throws IOException {
		return readable.read(buffer);
	}

	@Override
	public void close() throws IOException {
		// close writable first as this may trigger a flush() which writes some more
		try {
			writable.close();
		}
		finally {
			readable.close();
		}
	}

	@Override
	public long write(T buffer) throws IOException {
		return writable.write(buffer);
	}

	@Override
	public void flush() throws IOException {
		writable.flush();
	}
}
