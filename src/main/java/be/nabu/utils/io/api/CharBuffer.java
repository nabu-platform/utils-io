package be.nabu.utils.io.api;

import java.io.IOException;

public interface CharBuffer extends Buffer<CharBuffer> {
	public int read(char [] chars, int offset, int length) throws IOException;
	public int write(char [] chars, int offset, int length) throws IOException;
	public int read(char [] chars) throws IOException;
	public int write(char [] chars) throws IOException;
}
