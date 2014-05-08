package be.nabu.utils.io.api;

import java.io.Closeable;
import java.io.IOException;

public interface ReadableContainer<T extends Buffer<T>> extends Closeable {
	public long read(T buffer) throws IOException;
}
