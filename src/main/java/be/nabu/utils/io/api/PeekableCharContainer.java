package be.nabu.utils.io.api;

/**
 * Essentially the same as read but does not move the internal pointer so a peak() followed by a read() should return identical data
 */
public interface PeekableCharContainer extends ReadableCharContainer {

	public int peak(char [] chars);
	public int peak(char [] chars, int offset, int length);
	
}
