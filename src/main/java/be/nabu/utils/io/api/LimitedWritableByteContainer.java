package be.nabu.utils.io.api;

public interface LimitedWritableByteContainer extends WritableByteContainer {
	
	/**
	 * How much space is left to write to
	 * This can increase as the container is read from
	 */
	public long remainingSpace();
	
}
