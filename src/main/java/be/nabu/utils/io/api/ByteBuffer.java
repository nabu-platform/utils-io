package be.nabu.utils.io.api;

import java.io.IOException;

public interface ByteBuffer extends Buffer<ByteBuffer> {
	public int read(byte [] bytes, int offset, int length) throws IOException;
	public int write(byte [] bytes, int offset, int length) throws IOException;
	public int read(byte [] bytes) throws IOException;
	public int write(byte [] bytes) throws IOException;
}
