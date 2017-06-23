package be.nabu.utils.io.containers;

import java.io.IOException;

import be.nabu.utils.io.api.Buffer;
import be.nabu.utils.io.api.ReadableContainer;

public class EOFReadableContainer<T extends Buffer<T>> implements ReadableContainer<T> {

	private ReadableContainer<T> parent;
	private boolean eof = false;
	
	public EOFReadableContainer(ReadableContainer<T> parent) {
		this.parent = parent;
	}
	
	@Override
	public void close() throws IOException {
		parent.close();
		eof = true;
	}

	@Override
	public long read(T target) throws IOException {
		if (eof) {
			return -1;
		}
		long read = parent.read(target);
		if (read < 0)
			eof = true;
		return read <= 0 && eof ? -1 : read;
	}
	
	public boolean isEOF() {
		return eof;
	}
}
