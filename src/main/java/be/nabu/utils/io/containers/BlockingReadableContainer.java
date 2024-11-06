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
import java.util.Date;
import java.util.concurrent.TimeUnit;

import be.nabu.utils.io.api.Buffer;
import be.nabu.utils.io.api.ReadableContainer;

public class BlockingReadableContainer<T extends Buffer<T>> implements ReadableContainer<T> {

	private ReadableContainer<T> parent;
	private long timeout;
	
	public BlockingReadableContainer(ReadableContainer<T> parent) {
		this(parent, 0, null);
	}
	
	public BlockingReadableContainer(ReadableContainer<T> parent, long timeout, TimeUnit timeUnit) {
		this.parent = parent;
		if (timeUnit != null)
			this.timeout = timeUnit.convert(timeout, TimeUnit.MILLISECONDS);
	}
	
	@Override
	public void close() throws IOException {
		parent.close();
	}
	
	@Override
	public long read(T buffer) throws IOException {
		long read = 0;
		Date startedReading = timeout == 0 ? null : new Date();
		while (read == 0) {
			read = parent.read(buffer);
			if (read == 0) {
				// if there is no timeout, retry indefinitely
				if (timeout == 0)
					continue;
				// if we have exceeded the timeout, stop reading
				else if (new Date().getTime() - startedReading.getTime() > timeout)
					break;
			}
		}
		return read;
	}
}
