package be.nabu.utils.io.api;

import java.io.IOException;

public interface ResettableContainer<T extends Buffer<T>> extends ReadableContainer<T> {
	public void reset() throws IOException;
}
