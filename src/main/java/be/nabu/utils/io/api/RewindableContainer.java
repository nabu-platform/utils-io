package be.nabu.utils.io.api;

public interface RewindableContainer<T extends Buffer<T>> extends ReadableContainer<T> {
	public long rewind(long amount);
}
