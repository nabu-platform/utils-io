package be.nabu.utils.io.api;

import java.io.IOException;

public interface PushbackContainer<T extends Buffer<T>> extends ReadableContainer<T> {
	public void pushback(T buffer) throws IOException;
}