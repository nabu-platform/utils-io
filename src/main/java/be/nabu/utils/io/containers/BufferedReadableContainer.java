package be.nabu.utils.io.containers;

import java.io.IOException;

import be.nabu.utils.io.api.Buffer;
import be.nabu.utils.io.api.ReadableContainer;

public class BufferedReadableContainer<T extends Buffer<T>> implements ReadableContainer<T> {

	private ReadableContainer<T> parent;
	private T buffer;
	private boolean closed = false;
	
	public BufferedReadableContainer(ReadableContainer<T> parent, T buffer) {
		this.parent = parent;
		this.buffer = buffer;
	}
	
	@Override
	public void close() throws IOException {
		parent.close();
	}

	@Override
	public long read(T source) throws IOException {
		long totalRead = 0;
		while (source.remainingSpace() > 0) {
			if (buffer.remainingData() > 0)
				totalRead += buffer.read(source);
			else {
				long read = parent.read(buffer);
				if (read == -1) {
					closed = true;
					break;
				}
				else if (read == 0)
					break;
			}
		}
		return totalRead == 0 && closed ? -1 : totalRead;
	}
}
