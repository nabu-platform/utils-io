package be.nabu.utils.io.api;

import java.io.Closeable;

public interface ReadableCharContainer extends Closeable {
	
	public int read(char [] chars);
	public int read(char [] chars, int offset, int length);
	
}
