package be.nabu.utils.io.impl;

import java.util.ArrayList;
import java.util.List;

import be.nabu.utils.io.api.LimitedReadableCharContainer;
import be.nabu.utils.io.api.MarkableCharContainer;
import be.nabu.utils.io.api.PeekableCharContainer;
import be.nabu.utils.io.api.SkippableCharContainer;

/**
 * This class is NOT thread safe
 */
public class FragmentedReadableCharContainer implements LimitedReadableCharContainer, SkippableCharContainer, MarkableCharContainer, PeekableCharContainer {

	/**
	 * The size of each array in the list, each array has to be the same size
	 */
	int size;
	
	/**
	 * The writeIndex is the max array that data was written to
	 * The writeOffset is the max position in this array that data was written to
	 */
	int writeIndex, writeOffset,
		readIndex, readOffset,
		markIndex, markOffset;
	
	boolean closed = false;
	
	/**
	 * If you have read out data, should the array be released for gc?
	 */
	private boolean releaseRead = true;
	
	List<char[]> backingArrays = new ArrayList<char[]>();

	FragmentedReadableCharContainer(List<char[]> backingArrays, int size, int writeIndex, int writeOffset, int readIndex, int readOffset) {
		// make a copy of the original list so if we "release" data from the list, we don't alter the original list
		this.backingArrays = new ArrayList<char[]>(backingArrays);
		this.size = size;
		this.writeIndex = writeIndex;
		this.writeOffset = writeOffset;
		this.readIndex = readIndex;
		this.readOffset = readOffset;
	}
	
	private FragmentedReadableCharContainer parent;
	
	private FragmentedReadableCharContainer(FragmentedReadableCharContainer parent) {
		this.parent = parent;
		this.readIndex = parent.markIndex;
		this.readOffset = parent.markOffset;
		this.backingArrays = parent.backingArrays;
		this.writeIndex = parent.writeIndex;
		this.writeOffset = parent.writeOffset;
		this.size = parent.size;
	}
	
	int getWriteIndex() {
		return parent == null ? writeIndex : parent.getWriteIndex();
	}
	
	int getWriteOffset() {
		return parent == null ? writeOffset : parent.getWriteOffset();
	}
	
	@Override
	public int read(char [] chars) {
		return read(chars, 0, chars.length);
	}
	
	@Override
	public int read(char [] chars, int offset, int length) {
		int read = 0;
		while(length > 0) {
			char [] input = getInput();
			// no more data
			if (input == null) {
				if (read == 0 && closed)
					return -1;
				else
					return read;
			}
			int readLength = Math.min(length, (readIndex == getWriteIndex() ? getWriteOffset() : size) - readOffset);
			System.arraycopy(input, readOffset, chars, offset, readLength);
			length -= readLength;
			offset += readLength;
			readOffset += readLength;
			read += readLength;
		}
		return read;
	}
	
	private char [] getInput() {
		// nothing written yet
		if (backingArrays.size() == 0)
			return null;
		
		if (readOffset >= size) {
			// release without resetting the arraylist index
			if (releaseRead)
				backingArrays.set(readIndex, null);
			readIndex++;
			readOffset = 0;
		}
		// can not read past where it is written
		if (readIndex > getWriteIndex() || (readIndex == getWriteIndex() && readOffset >= getWriteOffset()))
			return null;
		else
			return backingArrays.get(readIndex);
	}
	
	public long size() {
		return (long) size * (long) getWriteIndex() + getWriteOffset();
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
		if (backingArrays.size() == 0)
			return 0;
		else
			// the amount of remaining backing arrays - whatever is already read in the first array - whatever is not yet written in the last array
			return (long) (backingArrays.size() - readIndex) * (long) size - readOffset - (size - getWriteOffset()); 
	}

	@Override
	public long skip(long amount) {
		long total = 0;
		while (amount > 0) {
			char [] input = getInput();
			if (input == null)
				break;
			int inputSize = readIndex == getWriteIndex() ? getWriteOffset() - readOffset : size - readOffset;
			inputSize = (int) Math.min(inputSize, amount);
			total += inputSize;
			readOffset += inputSize;
			amount -= inputSize;
		}
		return total;
	}

	@Override
	public void reset() {
		if (releaseRead)
			throw new IllegalStateException("No mark has been set, can not reset");
		readIndex = markIndex;
		readOffset = markOffset;
	}

	@Override
	public FragmentedReadableCharContainer clone() {
		if (releaseRead)
			throw new IllegalStateException("No mark has been set, can not clone");
		return new FragmentedReadableCharContainer(this);
	}

	@Override
	public void mark() {
		releaseRead = false;	
		markIndex = readIndex;
		markOffset = readOffset;
	}

	@Override
	public void unmark() {
		releaseRead = true;
		// forcefully release all data up to the read index
		for (int i = 0; i < readIndex; i++)
			backingArrays.set(i, null);
	}

	@Override
	public int peak(char[] chars) {
		return peak(chars, 0, chars.length);
	}

	@Override
	public int peak(char[] chars, int offset, int length) {
		// get the state as is
		int readOffset = this.readOffset;
		int readIndex = this.readIndex;
		boolean releaseRead = this.releaseRead;
		// set releaseRead explicitly to false so we don't lose the data
		this.releaseRead = false;
		// perform a read
		int amountRead = read(chars, offset, length);
		// restore original state
		this.readOffset = readOffset;
		this.readIndex = readIndex;
		this.releaseRead = releaseRead;
		// return the amount we read
		return amountRead;
	}
}
