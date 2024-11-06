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

package be.nabu.utils.io.buffers.chars;

import java.io.IOException;

import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.CharBuffer;
import be.nabu.utils.io.buffers.DynamicBuffer;

public class DynamicCharBuffer extends DynamicBuffer<CharBuffer, StaticCharBuffer> implements CharBuffer {

	public DynamicCharBuffer() {
		super(CharBufferFactory.getInstance());
	}

	public DynamicCharBuffer(int bufferSize) {
		super(CharBufferFactory.getInstance(), bufferSize);
	}

	@Override
	protected StaticCharBuffer newBuffer(int size) {
		return new StaticCharBuffer(size);
	}

	@Override
	public int read(char[] chars, int offset, int length) throws IOException {
		return (int) read(IOUtils.wrap(chars, offset, length, false));
	}

	@Override
	public int write(char[] chars, int offset, int length) throws IOException {
		return (int) write(IOUtils.wrap(chars, offset, length, true));
	}
	@Override
	public int read(char[] chars) throws IOException {
		return read(chars, 0, chars.length);
	}

	@Override
	public int write(char[] chars) throws IOException {
		return write(chars, 0, chars.length);
	}
	@Override
	public CharBufferFactory getFactory() {
		return (CharBufferFactory) super.getFactory();
	}

}
