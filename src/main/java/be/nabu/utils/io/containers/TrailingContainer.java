package be.nabu.utils.io.containers;

import java.io.IOException;

import be.nabu.utils.io.api.Buffer;
import be.nabu.utils.io.api.ReadableContainer;

public class TrailingContainer<T extends Buffer<T>> implements ReadableContainer<T> {

	private ReadableContainer<T> parent;
	private T buffer;
	private long trailSize;
	
	public TrailingContainer(ReadableContainer<T> parent, long trailSize) {
		this.parent = parent;
		this.trailSize = trailSize;
	}

	@Override
	public void close() throws IOException {
		parent.close();
	}

	@Override
	public long read(T target) throws IOException {
		long read = parent.read(target);
		
		if (buffer == null)
			buffer = target.getFactory().newInstance(trailSize, true);
		
		long amountToStore = Math.min(read, trailSize);
		if (amountToStore > 0) {
			long amountToSkip = amountToStore - buffer.remainingSpace();
			
			if (amountToSkip > 0)
				buffer.skip(amountToSkip);
			
			T copy = target.getFactory().newInstance(target.remainingData(), false);
			target.peek(copy);
			copy.skip(read - amountToStore);
			buffer.write(copy);
		}

		return read;
	}
	
	public ReadableContainer<T> getTrailing() {
		return buffer;
	}
}
