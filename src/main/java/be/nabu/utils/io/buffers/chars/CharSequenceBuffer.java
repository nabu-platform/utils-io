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

import be.nabu.utils.io.api.BufferFactory;
import be.nabu.utils.io.api.CharBuffer;

/**
 * Note that it is usually a _lot_ faster to simply convert to char array and use StaticCharBuffer
 * Tests indicate that StaticCharBuffer is 6 times faster for a string converted to char[] than wrapping this directly around the string even though the string to char is a copy
 */
public class CharSequenceBuffer implements CharBuffer {

	private CharSequence sequence;
	private int pointer;
	private char[] single = new char[1];

	public CharSequenceBuffer(CharSequence sequence) {
		this.sequence = sequence;
	}
	
	@Override
	public BufferFactory<CharBuffer> getFactory() {
		return CharBufferFactory.getInstance();
	}

	@Override
	public long read(CharBuffer buffer) throws IOException {
		return read(buffer, false);
	}
	@Override
	public int read(char[] chars, int offset, int length) throws IOException {
		int total = 0;
		while (length > 0 && pointer < sequence.length()) {
			chars[offset++] = sequence.charAt(pointer++);
			length--;
		}
		return total == 0 && length > 0 ? -1 : total;
	}

	@Override
	public int read(char[] chars) throws IOException {
		return read(chars, 0, chars.length);
	}

	private long read(CharBuffer buffer, boolean peek) throws IOException {
		long total = 0;
		int pointer = this.pointer;
		while (buffer.remainingSpace() > 0 && pointer < sequence.length()) {
			single[0] = sequence.charAt(pointer++);
			buffer.write(single);
		}
		if (!peek) {
			this.pointer = pointer;
		}
		return total == 0 && buffer.remainingSpace() > 0 ? -1 : total;
	}

	@Override
	public void close() throws IOException {
		// do nothing
	}

	@Override
	public void flush() throws IOException {
		// do nothing
	}

	@Override
	public long remainingData() {
		return sequence.length() - pointer;
	}

	@Override
	public long remainingSpace() {
		return 0;
	}

	@Override
	public void truncate() {
		pointer = sequence.length();
	}

	@Override
	public long skip(long amount) throws IOException {
		int newPointer = (int) Math.min(sequence.length(), pointer + amount);
		int skipAmount = newPointer - pointer;
		pointer = newPointer;
		return skipAmount;
	}

	@Override
	public long peek(CharBuffer buffer) throws IOException {
		return read(buffer, true);
	}

	@Override
	public long write(CharBuffer buffer) throws IOException {
		throw new IOException("The charsequence is closed");
	}
	@Override
	public int write(char[] chars, int offset, int length) throws IOException {
		throw new IOException("The charsequence is closed");
	}
	@Override
	public int write(char[] chars) throws IOException {
		throw new IOException("The charsequence is closed");
	}

}
