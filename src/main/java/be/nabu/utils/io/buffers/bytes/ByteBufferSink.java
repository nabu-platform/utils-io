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

import java.io.IOException;

import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.buffers.BufferSink;

public class ByteBufferSink extends BufferSink<ByteBuffer> implements ByteBuffer {

	public ByteBufferSink(long space) {
		super(ByteBufferFactory.getInstance(), space);
	}

	@Override
	public int read(byte[] bytes, int offset, int length) throws IOException {
		return (int) read(IOUtils.wrap(bytes, offset, length, false));
	}

	@Override
	public int write(byte[] bytes, int offset, int length) throws IOException {
		return (int) write(IOUtils.wrap(bytes, offset, length, true));
	}

	@Override
	public int read(byte[] bytes) throws IOException {
		return read(bytes, 0, bytes.length);
	}

	@Override
	public int write(byte[] bytes) throws IOException {
		return write(bytes, 0, bytes.length);
	}

}
