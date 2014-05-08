package be.nabu.utils.io.api;

public interface SkippableByteContainer extends ReadableByteContainer {
	
	/**
	 * Returns the actual amount skipped
	 */
	public long skip(long amount);
	
}
