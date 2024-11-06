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
import be.nabu.utils.io.api.Container;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.api.WritableContainer;

public class ComposedContainer<T extends Buffer<T>> implements Container<T> {

	private ReadableContainer<T> readable;
	private WritableContainer<T> writable;
	
	public ComposedContainer(ReadableContainer<T> readable, WritableContainer<T> writable) {
		this.readable = readable;
		this.writable = writable;
	}

	@Override
	public long read(T buffer) throws IOException {
		return readable.read(buffer);
	}

	@Override
	public void close() throws IOException {
		// close writable first as this may trigger a flush() which writes some more
		try {
			writable.close();
		}
		finally {
			readable.close();
		}
	}

	@Override
	public long write(T buffer) throws IOException {
		return writable.write(buffer);
	}

	@Override
	public void flush() throws IOException {
		writable.flush();
	}
}
