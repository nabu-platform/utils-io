package be.nabu.utils.io.api;

import java.io.IOException;

public interface SkippableContainer<T extends Buffer<T>> extends ReadableContainer<T> {
	
	/**
	 * Returns the actual amount skipped
	 */
	public long skip(long amount) throws IOException;
	
}
