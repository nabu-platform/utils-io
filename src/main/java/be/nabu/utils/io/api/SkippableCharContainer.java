package be.nabu.utils.io.api;

public interface SkippableCharContainer extends ReadableCharContainer {
	
	/**
	 * Returns the actual amount skipped
	 */
	public long skip(long amount);
	
}
