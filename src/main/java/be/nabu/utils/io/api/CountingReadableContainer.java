package be.nabu.utils.io.api;

public interface CountingReadableContainer<T extends Buffer<T>> extends ReadableContainer<T> {
	public long getReadTotal();
}
