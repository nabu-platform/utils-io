package be.nabu.utils.io.containers;

import java.io.IOException;

import be.nabu.utils.io.api.Buffer;
import be.nabu.utils.io.api.LimitedWritableContainer;
import be.nabu.utils.io.api.WritableContainer;

public class LimitedWritableContainerImpl<T extends Buffer<T>> implements LimitedWritableContainer<T> {

	private WritableContainer<T> parent;
	private long limit;
	private long writtenTotal;
	private T limitedBuffer;
		
	public LimitedWritableContainerImpl(WritableContainer<T> parent, long limit) {
		this.parent = parent;
		this.limit = limit;
	}

	@Override
	public long write(T target) throws IOException {
		long written = 0;
		// make sure we have no buffered data
		if (limitedBuffer == null || limitedBuffer.remainingData() == parent.write(limitedBuffer)) {
			if (limitedBuffer != null)
				limitedBuffer.truncate();
			
			if (limitedBuffer == null || limitedBuffer.remainingSpace() != target.remainingSpace() || remainingSpace() < target.remainingData())
				limitedBuffer = target.getFactory().newInstance(Math.min(remainingSpace(), target.remainingData()), false);
			
			written = target.read(limitedBuffer);
			if (written > 0) {
				parent.write(limitedBuffer);
				writtenTotal += written;
			}
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
