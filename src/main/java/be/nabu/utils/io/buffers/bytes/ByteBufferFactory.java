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

package be.nabu.utils.io.buffers.bytes;

import be.nabu.utils.io.api.BufferFactory;
import be.nabu.utils.io.api.ByteBuffer;

public class ByteBufferFactory implements BufferFactory<ByteBuffer> {

	private static ByteBufferFactory instance;
	
	public static ByteBufferFactory getInstance() {
		if (instance == null)
			instance = new ByteBufferFactory();
		return instance;
	}
	
	private ByteBufferFactory() {
		// hide
	}
	
	@Override
	public ByteBuffer newInstance(long size, boolean cyclic) {
		return cyclic ? new CyclicByteBuffer((int) size) : new StaticByteBuffer((int) size);
	}

	@Override
	public ByteBuffer newInstance() {
		return new DynamicByteBuffer();
	}

	@Override
	public ByteBuffer newSink(long size) {
		return new ByteBufferSink(size);
	}

	@Override
	public ByteBuffer limit(ByteBuffer buffer, Long maxRead, Long maxWrite) {
		return new LimitedByteBuffer(buffer, maxRead, maxWrite);
	}
}
