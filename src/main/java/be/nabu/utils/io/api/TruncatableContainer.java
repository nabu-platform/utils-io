package be.nabu.utils.io.api;

public interface TruncatableContainer<T extends Buffer<T>> extends WritableContainer<T> {
	public void truncate();
}
