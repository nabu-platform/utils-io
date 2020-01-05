package be.nabu.utils.io.containers;

import java.io.IOException;

import be.nabu.utils.io.api.Buffer;
import be.nabu.utils.io.api.ReadableContainer;

public class PushbackContainerImpl<T extends Buffer<T>> extends BasePushbackContainer<T> {

	private ReadableContainer<T> container;
	private boolean closed;
	private boolean read;
	
	public PushbackContainerImpl(ReadableContainer<T> container) {
		this.container = container;
	}
	
	@Override
	public long read(T target) throws IOException {
		read = true;
		long totalRead = 0;
		if (getBuffer() != null && getBuffer().remainingData() > 0) {
			totalRead += getBuffer().read(target);
		}
		if (target.remainingSpace() > 0) {
			long read = container.read(target);
			if (read == -1) {
				closed = true;
			}
			else {
				totalRead += read;
			}
		}
		return totalRead == 0 && closed ? -1 : totalRead;
	}

	@Override
	public void close() throws IOException {
		closed = true;
		container.close();
	}

	public boolean isRead() {
		return read;
	}

	public void setRead(boolean read) {
		this.read = read;
	}
}
