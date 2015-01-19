package be.nabu.utils.io.api;

public interface MarkableContainer<T extends Buffer<T>> extends ResettableContainer<T> {
	/**
	 * You can mark the container, any reset after this point will reset it to the given mark
	 * There is no read limit though specific implementations might provide this
	 */
	public void mark();
	public void unmark();
	/**
	 * At worst this is an unmark() + mark(), at best it can be a more performant option
	 */
	public void remark();
}
