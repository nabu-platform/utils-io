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

import be.nabu.utils.io.api.BufferFactory;
import be.nabu.utils.io.api.CharBuffer;

/**
 * This is a buffer that wraps around the incoming buffer
 * It simply limits the announced space remaining in this buffer for reading, that way no one should write too much to it
 */
public class LimitedCharBuffer implements CharBuffer {

	private CharBuffer original;
	private long maxWrite;
	private long maxRead;

	public LimitedCharBuffer(CharBuffer original, Long maxRead, Long maxWrite) {
		this.original = original;
		this.maxRead = maxRead == null ? Long.MAX_VALUE : maxRead;
		this.maxWrite = maxWrite == null ? Long.MAX_VALUE : maxWrite;
	}

	@Override
	public void close() throws IOException {
		original.close();
	}

	@Override
	public void flush() throws IOException {
		original.flush();
	}

	@Override
	public long remainingData() {
		return Math.min(original.remainingData(), maxRead);
	}

	@Override
	public long remainingSpace() {
		return Math.min(original.remainingSpace(), maxWrite);
	}

	@Override
	public void truncate() {
		original.truncate();
	}

	@Override
	public long skip(long amount) throws IOException {
		amount = original.skip(Math.min(amount, remainingData()));
		long skip = original.skip(amount);
		maxRead -= skip;
		return skip;
	}

	@Override
	public long read(CharBuffer buffer) throws IOException {
		if (buffer.remainingSpace() > remainingData()) {
			buffer = new LimitedCharBuffer(buffer, null, remainingData());
		}
		long read = original.read(buffer);
		maxRead -= read;
		return read;
	}

	@Override
	public long write(CharBuffer buffer) throws IOException {
		if (buffer.remainingData() > remainingSpace()) {
			buffer = new LimitedCharBuffer(buffer, remainingSpace(), null);
		}
		long write = original.write(buffer);
		maxWrite -= write;
		return write;
	}

	@Override
	public long peek(CharBuffer buffer) throws IOException {
		if (buffer.remainingSpace() > remainingData()) {
			buffer = new LimitedCharBuffer(buffer, null, remainingData());
		}
		return original.peek(buffer);
	}

	@Override
	public int read(char[] chars, int offset, int length) throws IOException {
		length = (int) Math.min(length, remainingData());
		int read = original.read(chars, offset, length);
		maxRead -= read;
		return read;
	}

	@Override
	public int write(char[] chars, int offset, int length) throws IOException {
		length = (int) Math.min(length, remainingSpace());
		int write = original.write(chars, offset, length);
		maxWrite -= write;
		return write;
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
	public BufferFactory<CharBuffer> getFactory() {
		return original.getFactory();
	}
}