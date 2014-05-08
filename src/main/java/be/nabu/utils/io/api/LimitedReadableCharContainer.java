package be.nabu.utils.io.api;

public interface LimitedReadableCharContainer extends ReadableCharContainer {
	
	/**
	 * How many bytes currently remain in the container for read
	 * This number may increase as new data is written to the container
	 */
	public long remainingData();
	
}
