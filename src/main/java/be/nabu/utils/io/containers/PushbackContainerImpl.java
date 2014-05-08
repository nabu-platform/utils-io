package be.nabu.utils.io.containers;

import java.io.IOException;

import be.nabu.utils.io.api.Buffer;
import be.nabu.utils.io.api.PushbackContainer;
import be.nabu.utils.io.api.ReadableContainer;

public class PushbackContainerImpl<T extends Buffer<T>> implements PushbackContainer<T> {

	private ReadableContainer<T> container;
	
	private T buffer;
	
	public PushbackContainerImpl(ReadableContainer<T> container) {
		this.container = container;
	}
	
	@Override
	public long read(T target) throws IOException {
		long totalRead = 0;
		while (target.remainingSpace() > 0) {
			long read = 0;
			if (buffer != null && buffer.remainingData() > 0)
				read = buffer.read(target);
			else
				read = container.read(target);
			if (read == -1) {
				if (totalRead == 0)
					totalRead = -1;
				break;
			}
			else if (read == 0)
				break;
			else
				totalRead += read;
		}
		return totalRead;
	}

	@Override
	public void close() throws IOException {
		container.close();
	}

	@Override
	public void pushback(T data) throws IOException {
		if (buffer == null)
			buffer = data.getFactory().newInstance();
		buffer.write(data);
	}

	public long getBufferSize() {
		return buffer == null ? 0 : buffer.remainingData();
	}
}
