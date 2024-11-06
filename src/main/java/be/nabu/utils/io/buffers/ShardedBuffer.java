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

public class ShardedBuffer<T extends Buffer<T>> extends CompositeBuffer<T> {
	
	public ShardedBuffer(BufferFactory<T> factory, boolean fifo) {
		super(factory, fifo, false);
	}

	@Override
	public long write(T buffer) throws IOException {
		if (buffers.isEmpty()) {
			shard();
		}
		Buffer<T> target = buffers.get(buffers.size() - 1);
		return target.write(buffer);
	}
	
	public void shard() {
		buffers.add(factory.newInstance());
	}

}
