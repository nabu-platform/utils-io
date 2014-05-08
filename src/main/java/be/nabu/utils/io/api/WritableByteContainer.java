package be.nabu.utils.io.api;

import java.io.Closeable;

/**
 * When the byte container is closed, any resources should be released
 * It is possible that someone reading from this container closes it, this also closes it for writing.
 * A container should return the amount of bytes that was written, in some cases you can't write it all out at once (e.g. when memory is limited)
 */
public interface WritableByteContainer extends Closeable {
	
	public int write(byte [] bytes);
	public int write(byte [] bytes, int offset, int length);
	
	public void flush();
	
}