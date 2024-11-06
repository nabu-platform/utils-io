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
import be.nabu.utils.io.api.PushbackContainer;
import be.nabu.utils.io.buffers.CompositeBuffer;
import be.nabu.utils.io.buffers.ShardedBuffer;

abstract public class BasePushbackContainer<T extends Buffer<T>> implements PushbackContainer<T> {
	
	public enum PushbackStrategy {
		// in the sharding strategy you only create a new buffer in certain circumstances as orchestrated by the code. this leads to a few big buffers in memory
		SHARDING,
		// in this mode you make a copy of the original and simply combine them. this leads to a lot of small buffers in memory
		COMPOSITE_COPY,
		// this mode is the same as the previous one but we don't make a copy. This should be used with caution as the original buffers are not guaranteed to be immutable
		// if applicable though, this is the fastest method
		COMPOSITE_ORIGINAL,
		// a single buffer is kept in memory. multiple writes are recombined into a single buffer. this is the default mode
		SINGLE
	}
	
	private T buffer;
	
	protected Buffer<T> getBuffer() {
		return buffer;
	}
	
	private PushbackStrategy strategy = PushbackStrategy.SINGLE;
	
	@SuppressWarnings("unchecked")
	@Override
	public void pushback(T data) throws IOException {
		switch(strategy) {
			case SHARDING:
				if (buffer == null) {
					buffer = (T) new ShardedBuffer<T>(data.getFactory(), false);
				}
				// if we have data remaining, shard the buffer
				if (buffer.remainingData() != 0) {
					((ShardedBuffer<T>) buffer).shard();
				}
				buffer.write(data);
			break;
			case COMPOSITE_COPY:
			case COMPOSITE_ORIGINAL:
				if (buffer == null) {
					buffer = (T) new CompositeBuffer<T>(data.getFactory(), false, strategy == PushbackStrategy.COMPOSITE_COPY);
				}
				buffer.write(data);
			break;
			default:
				if (buffer == null) {
					buffer = data.getFactory().newInstance();
				}
				// if we follow the first in, first out principle, any new push back data should come after the already pushed back data
				if (buffer.remainingData() == 0) {
					buffer.write(data);
				}
				// otherwise, any newly written data has to be read before old pushed back
				else {
					T newBuffer = data.getFactory().newInstance();
					newBuffer.write(data);
					newBuffer.write(buffer);
					buffer = newBuffer;
				}
		}
	}

	public long getBufferSize() {
		return buffer == null ? 0 : buffer.remainingData();
	}

	public PushbackStrategy getStrategy() {
		return strategy;
	}

	public void setStrategy(PushbackStrategy strategy) {
		this.strategy = strategy;
	}
}
