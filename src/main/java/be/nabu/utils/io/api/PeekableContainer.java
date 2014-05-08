package be.nabu.utils.io.api;

import java.io.IOException;

/**
 * Essentially the same as read but does not move the internal pointer so a peak() followed by a read() should return identical data
 */
public interface PeekableContainer<T extends Buffer<T>> extends ReadableContainer<T> {
	public long peek(T buffer) throws IOException;
}
