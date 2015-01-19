package be.nabu.utils.io.containers;

import java.io.IOException;

import be.nabu.utils.io.api.Buffer;
import be.nabu.utils.io.api.ReadableContainer;

public class PushbackContainerImpl<T extends Buffer<T>> extends BasePushbackContainer<T> {

	private ReadableContainer<T> container;
	
	public PushbackContainerImpl(ReadableContainer<T> container) {
		this.container = container;
	}
	
	@Override
	public long read(T target) throws IOException {
		long totalRead = 0;
		while (target.remainingSpace() > 0) {
			long read = 0;
			if (getBuffer() != null && getBuffer().remainingData() > 0)
				read = getBuffer().read(target);
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

}
