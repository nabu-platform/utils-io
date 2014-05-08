package be.nabu.utils.io.api;

public interface CountingWritableByteContainer extends WritableByteContainer {
	public long getWrittenTotal();
	public void resetWrittenTotal();
}
