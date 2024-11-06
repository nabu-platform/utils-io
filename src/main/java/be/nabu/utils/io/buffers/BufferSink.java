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

import be.nabu.utils.io.api.Buffer;
import be.nabu.utils.io.api.BufferFactory;

public class BufferSink<T extends Buffer<T>> implements Buffer<T> {

	private long space;
	private BufferFactory<T> factory;
	
	public BufferSink(BufferFactory<T> factory, long space) {
		this.factory = factory;
		this.space = space;
	}
	
	@Override
	public long read(T buffer) throws IOException {
		return 0;
	}

	@Override
	public void close() throws IOException {
		// do nothing
	}

	@Override
	public long write(T buffer) throws IOException {
		long amount = 0;
		if (space < 0)
			amount = buffer.remainingData();
		else {
			amount = Math.min(buffer.remainingData(), space);
			space -= amount;
		}
		return buffer.skip(amount);
	}

	@Override
	public void flush() throws IOException {
		// do nothing
	}

	@Override
	public long remainingData() {
		return 0;
	}

	@Override
	public long remainingSpace() {
		return space == -1 ? Long.MAX_VALUE : space;
	}

	@Override
	public void truncate() {
		// do nothing
	}

	@Override
	public long skip(long amount) {
		return 0;
	}

	@Override
	public BufferFactory<T> getFactory() {
		return factory;
	}

	@Override
	public long peek(T buffer) throws IOException {
		return 0;
	}

}
