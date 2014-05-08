package be.nabu.utils.io.impl;

import java.util.ArrayList;
import java.util.Date;

import be.nabu.utils.io.api.CharContainer;
import be.nabu.utils.io.api.IORuntimeException;
import be.nabu.utils.io.api.TruncatableCharContainer;

/**
 * This class is NOT thread safe
 */
public class DynamicCharContainer extends FragmentedReadableCharContainer implements CharContainer, TruncatableCharContainer {
	
	private Date lastModified = new Date();
	
	public DynamicCharContainer() {
		this(10240);
	}
	
	public DynamicCharContainer(int size) {
		super(new ArrayList<char[]>(), size, 0, 0, 0, 0);
	}
	
	@Override
	public int write(char [] chars) {
		return write(chars, 0, chars.length);
	}
	
	public void moveMark(int amount) {
		while (amount > size) {
			if (markIndex < writeIndex)
				markIndex++;
			else if (amount > size || markOffset + amount >= writeOffset)
				throw new IORuntimeException("Can not move mark past written data");
		}
		markOffset += amount;
	}
	
	@Override
	public int write(char [] chars, int offset, int length) {
		if (closed)
			throw new IllegalStateException("Can't write to a closed container");
		int amountToWrite = length;
		while (amountToWrite > 0) {
			char [] output = getOutput();
			int writeLength = Math.min(amountToWrite, output.length - writeOffset);
			System.arraycopy(chars, offset, output, writeOffset, writeLength); 
			amountToWrite -= writeLength;
			offset += writeLength;
			writeOffset += writeLength;
		}
		// only update lastModified if you actually wrote data
		if (length >  0)
			lastModified = new Date();
		return length;
	}
	
	private char[] getOutput() {
		// if we have filled an array, move to the next one
		if (writeOffset >= size) {
			writeIndex++;
			writeOffset = 0;
		}
		// create it if it doesn't exist yet
		if (backingArrays.size() <= writeIndex)
			backingArrays.add(new char[size]);
		
		return backingArrays.get(writeIndex);
	}
	
	@Override
	public void flush() {
		// do nothing
	}
	
	public Date getLastModified() {
		return lastModified;
	}

	@Override
	public void truncate() {
		writeIndex = 0;
		writeOffset = 0;
		backingArrays.clear();
	}
}
