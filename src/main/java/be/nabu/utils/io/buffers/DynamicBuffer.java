/*
* Copyright (C) 2014 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package be.nabu.utils.io.buffers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import be.nabu.utils.io.api.Buffer;
import be.nabu.utils.io.api.BufferFactory;
import be.nabu.utils.io.api.DuplicatableContainer;
import be.nabu.utils.io.api.PeekableContainer;
import be.nabu.utils.io.api.PositionableContainer;
import be.nabu.utils.io.api.ResettableContainer;
import be.nabu.utils.io.api.TruncatableContainer;

/**
 * This class is NOT thread safe
 */
abstract public class DynamicBuffer<T extends Buffer<T>, S extends Buffer<T> & DuplicatableContainer<T, S> & ResettableContainer<T> & PositionableContainer<T> & PeekableContainer<T>> extends FragmentedReadableContainer<T, S> implements Buffer<T>, TruncatableContainer<T> {
	
	private Date lastModified = new Date();
	
	public DynamicBuffer(BufferFactory<T> factory) {
		this(factory, 10240);
	}
	
	public DynamicBuffer(BufferFactory<T> factory, int bufferSize) {
		super(factory, new ArrayList<S>(), bufferSize);
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
		backingArrays.clear();
		readIndex = 0;
		markIndex = 0;
	}
	
	@Override
	public long remainingSpace() {
		return Long.MAX_VALUE;
	}

	private S getWritableBackingArray() {
		if (backingArrays.isEmpty() || backingArrays.get(backingArrays.size() - 1).remainingSpace() == 0)
			backingArrays.add(newBuffer(bufferSize));
		return backingArrays.get(backingArrays.size() - 1);
	}
	
	@Override
	public long write(T buffer) throws IOException {
		if (closed)
			throw new IllegalStateException("Can't write to a closed container");
		
		long totalWritten = 0;
		
		while (buffer.remainingData() > 0)
			totalWritten += getWritableBackingArray().write(buffer);

		// only update lastModified if you actually wrote data
		if (totalWritten >  0)
			lastModified = new Date();
		
		return totalWritten;
	}
	
	protected abstract S newBuffer(int size);
}
