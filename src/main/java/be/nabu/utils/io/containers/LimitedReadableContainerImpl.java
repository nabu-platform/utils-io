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
import be.nabu.utils.io.api.LimitedReadableContainer;
import be.nabu.utils.io.api.ReadableContainer;

public class LimitedReadableContainerImpl<T extends Buffer<T>> implements LimitedReadableContainer<T> {

	private ReadableContainer<T> parent;
	private long limit;
	private long readTotal;
	
	public <S extends ReadableContainer<T> & LimitedReadableContainer<T>> LimitedReadableContainerImpl(S parent) {
		this.parent = parent;
		limit = parent.remainingData();
	}
	
	public LimitedReadableContainerImpl(ReadableContainer<T> parent, long limit) {
		this.parent = parent;
		this.limit = limit;
	}

	@Override
	public void close() throws IOException {
		parent.close();
	}

	@Override
	public long remainingData() {
		return limit - readTotal;
	}

	@Override
	public long read(T target) throws IOException {
		long read = parent.read(target.getFactory().limit(target, null, remainingData()));
		if (read > 0) {
			readTotal += read;
		}
		return read == 0 && readTotal == limit ? -1 : read;
	}
}
