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

package be.nabu.utils.io.containers;

import java.io.IOException;

import be.nabu.utils.io.api.Buffer;
import be.nabu.utils.io.api.CountingReadableContainer;
import be.nabu.utils.io.api.ReadableContainer;

public class CountingReadableContainerImpl<T extends Buffer<T>> implements CountingReadableContainer<T> {

	private ReadableContainer<T> parent;
	private long readTotal = 0;
	
	public CountingReadableContainerImpl(ReadableContainer<T> parent) {
		this(parent, 0);
	}
	
	public CountingReadableContainerImpl(ReadableContainer<T> parent, long alreadyRead) {
		this.parent = parent;
		this.readTotal = alreadyRead;
	}
	
	@Override
	public void close() throws IOException {
		parent.close();
	}

	@Override
	public long read(T target) throws IOException {
		long read = parent.read(target);
		if (read > 0)
			readTotal += read;
		return read;
	}
	
	@Override
	public long getReadTotal() {
		return readTotal;
	}
	
	public void add(long value) {
		readTotal += value;
	}

	public void setReadTotal(long value) {
		this.readTotal = value;
	}
}
