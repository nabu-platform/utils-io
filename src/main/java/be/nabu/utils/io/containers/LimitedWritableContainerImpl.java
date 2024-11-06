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
import be.nabu.utils.io.api.LimitedWritableContainer;
import be.nabu.utils.io.api.WritableContainer;

public class LimitedWritableContainerImpl<T extends Buffer<T>> implements LimitedWritableContainer<T> {

	private WritableContainer<T> parent;
	private long limit;
	private long writtenTotal;
		
	public LimitedWritableContainerImpl(WritableContainer<T> parent, long limit) {
		this.parent = parent;
		this.limit = limit;
	}

	@Override
	public long write(T target) throws IOException {
		long written = parent.write(target.getFactory().limit(target, remainingSpace(), null));
		if (written > 0) {
			writtenTotal += written;
		}
		return written;
	}

	@Override
	public void close() throws IOException {
		parent.close();
	}

	@Override
	public long remainingSpace() {
		return limit - writtenTotal;
	}

	@Override
	public void flush() throws IOException {
		parent.flush();
	}

}
