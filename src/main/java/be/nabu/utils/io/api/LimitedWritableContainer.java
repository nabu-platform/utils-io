package be.nabu.utils.io.api;

public interface LimitedWritableContainer<T extends Buffer<T>> extends WritableContainer<T> {
	
	/**
	 * How much space is left to write to
	 * This can increase as the container is read from
	 */
	public long remainingSpace();
	
}
