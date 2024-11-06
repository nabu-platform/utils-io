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
import java.util.List;

import be.nabu.utils.io.api.Buffer;
import be.nabu.utils.io.api.BufferFactory;

public class CompositeBuffer<T extends Buffer<T>> implements Buffer<T> {
	
	protected BufferFactory<T> factory;
	protected List<Buffer<T>> buffers = new ArrayList<Buffer<T>>();
	private boolean fifo;
	private boolean copy;

	public CompositeBuffer(BufferFactory<T> factory, boolean fifo, boolean copy) {
		this.factory = factory;
		this.fifo = fifo;
	}

	@Override
	public long read(T target) throws IOException {
		long total = 0;
		while(target.remainingSpace() > 0 && !buffers.isEmpty()) {
			int index = fifo ? 0 : buffers.size() - 1;
			Buffer<T> buffer = buffers.get(index);
			total += buffer.read(target);
			if (buffer.remainingData() == 0) {
				buffers.remove(index);
			}
		}
		return total;
	}

	@Override
	public void close() throws IOException {
		IOException exception = null;
		for (Buffer<T> buffer : buffers) {
			try {
				buffer.close();
			}
			catch (IOException e) {
				exception = e;
			}
		}
		if (exception != null) {
			throw exception;
		}
	}

	@Override
	public long write(T buffer) throws IOException {
		if (copy) {
			Buffer<T> target = factory.newInstance();
			long written = target.write(buffer);
			if (written > 0) {
				buffers.add(target);
			}
			return written;
		}
		else {
			buffers.add(buffer);
			return buffer.remainingData();
		}
	}

	@Override
	public void flush() throws IOException {
		// do nothing
	}

	@Override
	public long remainingData() {
		long total = 0;
		for (Buffer<T> buffer : buffers) {
			total += buffer.remainingData();
		}
		return total;
	}

	@Override
	public long remainingSpace() {
		return Long.MAX_VALUE;
	}

	@Override
	public void truncate() {
		buffers.clear();
	}

	@Override
	public long skip(long amount) throws IOException {
		while (amount > 0 && !buffers.isEmpty()) {
			int index = fifo ? 0 : buffers.size() - 1;
			Buffer<T> buffer = buffers.get(index);
			amount -= buffer.skip(amount);
			if (buffer.remainingData() == 0) {
				buffers.remove(index);
			}
		}
		return 0;
	}

	@Override
	public long peek(T target) throws IOException {
		long total = 0;
		while(target.remainingSpace() > 0 && !buffers.isEmpty()) {
			int index = fifo ? 0 : buffers.size() - 1;
			Buffer<T> buffer = buffers.get(index);
			total += buffer.peek(target);
		}
		return total;
	}

	@Override
	public BufferFactory<T> getFactory() {
		return factory;
	}
}
