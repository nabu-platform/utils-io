package be.nabu.utils.io.api;

import java.io.Closeable;

public interface ReadableByteContainer extends Closeable {

	public int read(byte [] bytes);
	public int read(byte [] bytes, int offset, int length);

}