package be.nabu.utils.io.buffers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import be.nabu.utils.io.api.Buffer;
import be.nabu.utils.io.api.BufferFactory;
import be.nabu.utils.io.api.DuplicatableContainer;
import be.nabu.utils.io.api.LimitedReadableContainer;
import be.nabu.utils.io.api.MarkableContainer;
import be.nabu.utils.io.api.PeekableContainer;
import be.nabu.utils.io.api.PositionableContainer;
import be.nabu.utils.io.api.ResettableContainer;
import be.nabu.utils.io.api.SkippableContainer;

public class FragmentedReadableContainer<T extends Buffer<T>, S extends Buffer<T> & DuplicatableContainer<T, S> & ResettableContainer<T> & PositionableContainer<T> & PeekableContainer<T>> implements LimitedReadableContainer<T>, SkippableContainer<T>, MarkableContainer<T>, PeekableContainer<T> {

	/**
	 * The size of each buffer in the list, they have to be the same size
	 */
	protected int bufferSize;
	
	protected int readIndex, markIndex;
	private long markOffset;
	
	protected boolean closed = false;
	
	private BufferFactory<T> factory;
	
	/**
	 * If you have read out data, should the buffer be released for gc?
	 */
	private boolean releaseRead = true;
	
	protected List<S> backingArrays = null;

	/**
	 * Instead of using your own state, you can depend on a parent which means you can read from the parents data
	 */
	private FragmentedReadableContainer<T, S> parent;
	
	/**
	 * This constructor can be used to start a read view of the given list of backing arrays 
	 */
	protected FragmentedReadableContainer(BufferFactory<T> bufferFactory, List<S> backingArrays, int bufferSize) {
		this.factory = bufferFactory;
		this.backingArrays = backingArrays;
		this.bufferSize = bufferSize;
	}
	
	/**
	 * This constructor can be used to start a read-only view of a list of backing arrays managed by another instance 
	 */
	private FragmentedReadableContainer(BufferFactory<T> bufferFactory, FragmentedReadableContainer<T, S> parent) {
		this.factory = bufferFactory;
		this.parent = parent;
		this.readIndex = parent.markIndex;
		this.bufferSize = parent.bufferSize;
		this.backingArrays = new ArrayList<S>();
	}
	
	private S getBackingArray(int i) {
		// fill the current backing arrays with proxies where necessary
		if (i >= backingArrays.size() && parent != null) {
			for (int j = Math.max(backingArrays.size() - 1, 0); j < parent.backingArrays.size(); j++) {
				if (j >= backingArrays.size())
					backingArrays.add(parent.getBackingArray(j).duplicate());
				else
					backingArrays.set(j, parent.getBackingArray(j).duplicate());
				// reset the duplicate so it reads everything
				try {
					backingArrays.get(j).reset();
				}
				catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
		return backingArrays.isEmpty() || i >= backingArrays.size() ? null : backingArrays.get(i);
	}
	
	private int amountOfBackingArrays() {
		return parent == null ? backingArrays.size() : parent.amountOfBackingArrays();
	}
	
	@Override
	public void close() {
		closed = true;
	}
	
	public void reopen() {
		closed = false;
	}

	@Override
	public long remainingData() {
		if (amountOfBackingArrays() == 0)
			return 0;
		else {
			long amount = 0;
			for (int i = readIndex; i < amountOfBackingArrays(); i++)
				amount += getBackingArray(i).remainingData();
			return amount;
		}
	}

	@Override
	public long skip(long amount) throws IOException {
		return read(getFactory().newSink(amount));
	}

	@Override
	public void reset() throws IOException {
		if (releaseRead)
			throw new IllegalStateException("No mark has been set, can not reset");
		readIndex = markIndex;
		for (int i = readIndex; i >= markIndex; i--) {
			getBackingArray(readIndex).reset();
			readIndex = i;
		}
		getBackingArray(readIndex).skip(markOffset);
	}
	
	/**
	 * Creates a new readable which is based on the marked position of this container
	 */
	@Override
	public FragmentedReadableContainer<T, S> clone() {
		if (releaseRead)
			throw new IllegalStateException("No mark has been set, can not clone");
		return new FragmentedReadableContainer<T, S>(factory, this);
	}

	@Override
	public void mark() {
		releaseRead = false;	
		markIndex = readIndex;
		markOffset = amountOfBackingArrays() == 0 ? 0 : getBackingArray(readIndex).position();
	}

	@Override
	public void unmark() {
		releaseRead = true;
		// forcefully release all data up to the read index
		for (int i = 0; i < readIndex; i++)
			backingArrays.set(i, null);
	}

	@Override
	public long peek(T buffer) throws IOException {
		return read(buffer, true);
	}

	@Override
	public long read(T buffer) throws IOException {
		return read(buffer, false);
	}

	private long read(T buffer, boolean peek) throws IOException {
		if (remainingData() == 0 && closed)
			return -1;
		long totalRead = 0;
		int readIndex = this.readIndex;
		while(buffer.remainingSpace() > 0) {
			S data = getBackingArray(readIndex);
			if (data == null || data.remainingData() == 0) {
				if (readIndex >= amountOfBackingArrays() - 1) {
					if (totalRead == 0 && closed) {
						totalRead = -1;
						break;
					}
					else
						break;
				}
				else {
					// release the backing array without changing the index count of the list
					if (releaseRead && !peek)
						backingArrays.set(readIndex, null);
					readIndex++;
					data = getBackingArray(readIndex);
				}
			}
			// if we are peeking the data, the "buffer" has either more or less (or equal) space as the data
			// if more space, we need to do readIndex++ to skip to the next data set
			// if less (or equal) space, the buffer will fill out meaning remainingSpace() becomes 0 ending the loop
			if (peek) {
				totalRead += data.peek(buffer);
				readIndex++;
			}
			else
				totalRead += data.read(buffer);
		}
		if (!peek)
			this.readIndex = readIndex;
		return totalRead;
	}

	public BufferFactory<T> getFactory() {
		return factory;
	}
}
