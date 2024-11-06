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

import javax.xml.bind.annotation.XmlTransient;

import be.nabu.utils.io.api.BufferFactory;
import be.nabu.utils.io.api.CharBuffer;
import be.nabu.utils.io.buffers.LimitedCharBuffer;

public class CharBufferFactory implements BufferFactory<CharBuffer> {

	private static CharBufferFactory instance;

	@XmlTransient
	public static CharBufferFactory getInstance() {
		if (instance == null)
			instance = new CharBufferFactory();
		return instance;
	}
	
	private CharBufferFactory() {
		// hide
	}
	
	@Override
	public CharBuffer newInstance(long size, boolean cyclic) {
		return cyclic ? new CyclicCharBuffer((int) size) : new StaticCharBuffer((int) size);
	}

	@Override
	public CharBuffer newInstance() {
		return new DynamicCharBuffer();
	}

	@Override
	public CharBuffer newSink(long size) {
		return new CharBufferSink(size);
	}

	@Override
	public CharBuffer limit(CharBuffer buffer, Long maxRead, Long maxWrite) {
		return new LimitedCharBuffer(buffer, maxRead, maxWrite);
	}
}
