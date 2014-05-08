package be.nabu.utils.io.api;

public interface MarkableByteContainer extends ResettableByteContainer {
	/**
	 * You can mark the container, any reset after this point will reset it to the given mark
	 * There is no read limit, 
	 */
	public void mark();
	public void unmark();
}
