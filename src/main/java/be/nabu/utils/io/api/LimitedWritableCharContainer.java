package be.nabu.utils.io.api;

public interface LimitedWritableCharContainer extends WritableCharContainer {
	
	/**
	 * How much space is left to write to
	 * This can increase as the container is read from
	 */
	public long remainingSpace();
	
}
