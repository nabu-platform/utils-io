package be.nabu.utils.io.api;

public interface PositionableContainer<T extends Buffer<T>> extends ReadableContainer<T> {
	public long position();
}
