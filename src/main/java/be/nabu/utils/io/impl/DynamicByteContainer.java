package be.nabu.utils.io.impl;

import java.util.ArrayList;
import java.util.Date;

import be.nabu.utils.io.api.ByteContainer;
import be.nabu.utils.io.api.IORuntimeException;
import be.nabu.utils.io.api.TruncatableByteContainer;

/**
 * This class is NOT thread safe
 */
public class DynamicByteContainer extends FragmentedReadableByteContainer implements ByteContainer, TruncatableByteContainer {
	
	private Date lastModified = new Date();
	
	public DynamicByteContainer() {
		this(10240);
	}
	
	public DynamicByteContainer(int size) {
		super(new ArrayList<byte[]>(), size, 0, 0, 0, 0);
	}
	
	@Override
	public int write(byte [] bytes) {
		return write(bytes, 0, bytes.length);
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
	public int write(byte [] bytes, int offset, int length) {
		if (closed)
			throw new IllegalStateException("Can't write to a closed container");
		int amountToWrite = length;
		while (amountToWrite > 0) {
			byte [] output = getOutput();
			int writeLength = Math.min(amountToWrite, output.length - writeOffset);
			System.arraycopy(bytes, offset, output, writeOffset, writeLength); 
			amountToWrite -= writeLength;
			offset += writeLength;
			writeOffset += writeLength;
		}
		// only update lastModified if you actually wrote data
		if (length >  0)
			lastModified = new Date();
		return length;
	}
	
	private byte[] getOutput() {
		// if we have filled an array, move to the next one
		if (writeOffset >= size) {
			writeIndex++;
			writeOffset = 0;
		}
		// create it if it doesn't exist yet
		if (backingArrays.size() <= writeIndex)
			backingArrays.add(new byte[size]);
		
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
