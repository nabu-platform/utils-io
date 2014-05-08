package be.nabu.utils.io.api;

/**
 * Essentially the same as read but does not move the internal pointer so a peak() followed by a read() should return identical data
 */
public interface PeekableByteContainer extends ReadableByteContainer {

	public int peak(byte [] bytes);
	public int peak(byte [] bytes, int offset, int length);
	
}
