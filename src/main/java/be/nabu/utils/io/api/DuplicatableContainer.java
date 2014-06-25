package be.nabu.utils.io.api;

public interface DuplicatableContainer<T extends Buffer<T>, S extends ReadableContainer<T>> extends ReadableContainer<T> {
	public S duplicate(boolean reset);
}
