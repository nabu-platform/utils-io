package be.nabu.utils.io.api;

import java.io.Closeable;

public interface WritableCharContainer extends Closeable {
	
	public int write(char [] chars);
	public int write(char [] chars, int offset, int length);
	
	public void flush();
	
}
