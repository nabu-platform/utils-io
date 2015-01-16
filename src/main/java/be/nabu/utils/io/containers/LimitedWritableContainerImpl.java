package be.nabu.utils.io.containers;

import java.io.IOException;

import be.nabu.utils.io.api.Buffer;
import be.nabu.utils.io.api.LimitedWritableContainer;
import be.nabu.utils.io.api.WritableContainer;

public class LimitedWritableContainerImpl<T extends Buffer<T>> implements LimitedWritableContainer<T> {

	private WritableContainer<T> parent;
	private long limit;
	private long writtenTotal;
		
	public LimitedWritableContainerImpl(WritableContainer<T> parent, long limit) {
		this.parent = parent;
		this.limit = limit;
	}

	@Override
	public long write(T target) throws IOException {
		long written = parent.write(target.getFactory().limit(target, remainingSpace(), null));
		if (written > 0) {
			writtenTotal += written;
		}
		return written;
	}

	@Override
	public void close() throws IOException {
		parent.close();
	}

	@Override
	public long remainingSpace() {
		return limit - writtenTotal;
	}

	@Override
	public void flush() throws IOException {
		parent.flush();
	}

}
