package be.nabu.utils.io.api;

public interface CountingWritableContainer<T extends Buffer<T>> extends WritableContainer<T> {
	public long getWrittenTotal();
}
