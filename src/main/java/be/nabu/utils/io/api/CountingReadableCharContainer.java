package be.nabu.utils.io.api;

public interface CountingReadableCharContainer extends ReadableCharContainer {
	public long getReadTotal();
	public void resetReadTotal();
}
