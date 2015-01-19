package be.nabu.utils.io.containers;

import java.io.IOException;

import be.nabu.utils.io.api.Buffer;
import be.nabu.utils.io.api.MarkableContainer;
import be.nabu.utils.io.api.ReadableContainer;

public class LimitedMarkableContainer<T extends Buffer<T>> extends BasePushbackContainer<T> implements MarkableContainer<T> {

	/**
	 * The backing container contains all the data gleaned from the parent
	 */
	private T backingContainer;
	
	/**
	 * The reset container contains a copy of the backingContainer but with a state that matches the reader
	 */
	private T resetContainer;
	
	private ReadableContainer<T> parent;
	private boolean reset = false, marked = false;
	
	/**
	 * If the limit is 0 or smaller, there is no limit
	 */
	private long readLimit;
	
	public LimitedMarkableContainer(ReadableContainer<T> parent, long readLimit) {
		this.parent = parent;
		this.readLimit = readLimit;
	}
	
	@Override
	public void reset() throws IOException {
		if (!marked)
			throw new IllegalStateException("No mark has been set or the readLimit has been exceeded, can not reset");
		reset = true;
		resetContainer = null;
		// we have to truncate pushed back data, it will still be available in the main container
		if (getBuffer() != null && getBuffer().remainingData() > 0) {
			getBuffer().truncate();
		}
	}

	@Override
	public void pushback(T data) throws IOException {
		// if you push back data, it has to be put onto the backing container
		// if it's too much, unmark()
		if (marked) {
			T newBackingContainer = readLimit == 0 ? data.getFactory().newInstance() : data.getFactory().newInstance(readLimit, false);
			if (data.peek(newBackingContainer) != data.remainingData()) {
				unmark();
			}
			else if (backingContainer != null && backingContainer.remainingData() != newBackingContainer.write(backingContainer)) {
				unmark();
			}
			else {
				backingContainer = newBackingContainer;
			}
		}
		super.pushback(data);
	}

	@Override
	public long read(T target) throws IOException {
		long totalRead = 0;

		if (getBuffer() != null && getBuffer().remainingData() > 0) {
			totalRead += getBuffer().read(target);
		}
		
		while (target.remainingSpace() > 0) {
			long read = 0;
			// there is no backing container yet, create it
			if (marked && backingContainer == null)
				backingContainer = readLimit == 0 ? target.getFactory().newInstance() : target.getFactory().newInstance(readLimit, false);
				
			// if we have marked it but are reading past the buffer size, unmark
			else if (marked && !reset && backingContainer.remainingSpace() == 0)
				unmark();
			
			// if we have reset but the reset container is empty, fill it with the backing container data
			if (reset && resetContainer == null) {
				resetContainer = target.getFactory().newInstance(backingContainer.remainingData(), false);
				backingContainer.peek(resetContainer);
			}
			
			// if we have reset and there is still data in it, use that
			if (reset && resetContainer.remainingData() > 0) {
				read = resetContainer.read(target);
				if (resetContainer.remainingData() == 0) {
					reset = false;
					resetContainer = null;
				}
			}
			else if (marked) {
				// for optimization: if the target buffer has no data inside of it, we can read to it directly because we can peek the result
				// if it does have data, we would need some structure to skip certain things which in the end is too much overhead, might as well create a new container then
				// note that in the overwhelming amount of usecases (90% in generic tests), the target is empty when we get here
				if (target.remainingData() == 0) {
					read = parent.read(target);
					// the amount we read is too big to be stored in the backing container
					if (read > backingContainer.remainingSpace()) {
						unmark();
					}
					else {
						target.peek(backingContainer);
					}
				}
				else {
					T buffer = target.getFactory().newInstance(target.remainingSpace(), false);
					read = parent.read(buffer);
					// couldn't push everything to backing container, presumably because limit is reached, unset
					if (buffer.peek(backingContainer) < buffer.remainingData()) {
						unmark();
					}
					target.write(buffer);
				}
			}
			else {
				read = parent.read(target);
			}
			
			// make sure this signals -1 if no data was read
			if (read == -1) {
				if (totalRead == 0)
					totalRead = read;
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
		if (backingContainer != null)
			backingContainer.close();
		parent.close();
	}

	@Override
	public void mark() {
		// reset current container
		if (marked)
			unmark();
		marked = true;
	}

	@Override
	public void remark() {
		if (marked) {
			if (backingContainer != null) {
				backingContainer.truncate();
			}
			// if there is something left in the pushback container, you remarked while reading pushback data
			// TODO: is it possible that something you pushed back is in the reset container as well? because the reset() container is a copy of the backing container and pushback is sent to both pushback and backing
			// although an actual reset() clears the pushback data... perhaps an odd combination of mark+reset+pushback could trigger this though...
			if (getBuffer() != null && getBuffer().remainingData() > 0) {
				try {
					getBuffer().peek(backingContainer);
				}
				catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
			// if there is something still in the reset container, you remarked in a resetted part
			if (resetContainer != null && resetContainer.remainingData() > 0) {
				try {
					resetContainer.peek(backingContainer);
				}
				catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
			else {
				resetContainer = null;
				reset = false;
			}
		}
	}

	@Override
	public void unmark() {
		if (marked) {
			marked = false;
			backingContainer = null;
			resetContainer = null;
			reset = false;
		}
	}
}
