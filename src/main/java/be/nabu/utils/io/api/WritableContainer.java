package be.nabu.utils.io.api;

import java.io.Closeable;
import java.io.IOException;

public interface WritableContainer<T extends Buffer<T>> extends Closeable {
	public long write(T buffer) throws IOException;
	public void flush() throws IOException;
}
