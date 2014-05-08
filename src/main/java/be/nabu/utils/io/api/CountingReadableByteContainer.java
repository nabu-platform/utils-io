package be.nabu.utils.io.api;

public interface CountingReadableByteContainer extends ReadableByteContainer {
	public long getReadTotal();
	public void resetReadTotal();
}
