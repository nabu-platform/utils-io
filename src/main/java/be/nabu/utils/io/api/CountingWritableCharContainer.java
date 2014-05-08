package be.nabu.utils.io.api;

public interface CountingWritableCharContainer extends WritableCharContainer {
	public long getWrittenTotal();
	public void resetWrittenTotal();
}
