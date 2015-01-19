package be.nabu.utils.io.containers;

import java.io.IOException;

import be.nabu.utils.io.api.Buffer;
import be.nabu.utils.io.api.CountingReadableContainer;
import be.nabu.utils.io.api.MarkableContainer;
import be.nabu.utils.io.api.ReadableContainer;

public class LimitedMarkableContainer<T extends Buffer<T>> extends BasePushbackContainer<T> implements MarkableContainer<T>, CountingReadableContainer<T> {

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
	
	private long alreadyRead;
	
	private long markPosition;
	
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

	public long getMarkedPosition() {
		return markPosition;
	}
	
	public long moveMarkAbsolute(long readIndex) throws IOException {
		return moveMarkRelative(readIndex - markPosition);
	}
	
	public long moveMarkRelative(long offset) throws IOException {
		if (offset < 0) {
			throw new IOException("Can not move the mark backwards");
		}
		// move the mark ahead by a certain amount
		long markMoved = marked && backingContainer != null
			? backingContainer.skip(offset)
			: 0;
		markPosition += markMoved;
		return markMoved;
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
				backingContainer = readLimit == 0 ? target.getFactory().newInstance() : target.getFactory().newInstance(readLimit, true);
				
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
					if (read > 0) {
						alreadyRead += read;
					}
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
					if (read > 0) {
						alreadyRead += read;
					}
					// couldn't push everything to backing container, presumably because limit is reached, unset
					if (buffer.peek(backingContainer) < buffer.remainingData()) {
						unmark();
					}
					target.write(buffer);
					if (buffer.remainingData() > 0) {
						throw new IOException("Could not read everything into target: " + buffer.remainingData());
					}
				}
			}
			else {
				read = parent.read(target);
				if (read > 0) {
					alreadyRead += read;
				}
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
		markPosition = getReadTotal();
	}

	@Override
	public void remark() {
		if (marked) {
			if (backingContainer != null) {
				// if the reset container still contains some data, you remarked() while you were in a reset
				// we must retain this data
				long amountToRetainForReset = resetContainer != null && resetContainer.remainingData() > 0 ? resetContainer.remainingData() : 0;
				// if you have pushed back information, we assume you don't want it gone on remark() so check that as well
				long amountToRetainForPushback = getBuffer() != null && getBuffer().remainingData() > 0 ? getBuffer().remainingData() : 0;

				// we need to retain both the resetted data and the pushed back data
				long amountToActuallyRetain = amountToRetainForPushback + amountToRetainForReset;

				// if it is 0, we don't have to retain anything
				if (amountToActuallyRetain == 0) {
					markPosition = getReadTotal();
					backingContainer.truncate();
				}
				else if (amountToActuallyRetain < 0 || amountToActuallyRetain > backingContainer.remainingData()) {
					throw new RuntimeException("You pushed back more than is stored from the mark point, can not remark() without losing data");
				}
				else {
					try {
						markPosition += backingContainer.skip(backingContainer.remainingData() - amountToActuallyRetain);
					}
					catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
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

	@Override
	public long getReadTotal() {
		return alreadyRead;
	}
}
