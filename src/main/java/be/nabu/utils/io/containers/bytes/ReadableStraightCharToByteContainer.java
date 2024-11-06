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

package be.nabu.utils.io.containers.bytes;

import java.io.IOException;

import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.CharBuffer;
import be.nabu.utils.io.api.ReadableContainer;

/**
 * Performs a 1-1 mapping from chars to bytes where the exact byte value is translated from a char
 * This is not the same as code page 437 but instead conforms to http://www.unicode.org/charts/PDF/U0080.pdf
 */
public class ReadableStraightCharToByteContainer implements ReadableContainer<ByteBuffer> {

	private ReadableContainer<CharBuffer> chars;

	private char [] singleChar = new char[1];
	private byte [] singleByte = new byte[1];
	
	public ReadableStraightCharToByteContainer(ReadableContainer<CharBuffer> chars) {
		this.chars = chars;
	}

	@Override
	public void close() throws IOException {
		chars.close();
	}

	@Override
	public long read(ByteBuffer target) throws IOException {
		long totalRead = 0;
		while (target.remainingSpace() > 0) {
			long read = chars.read(IOUtils.wrap(singleChar, false));
			if (read == -1)
				return totalRead == 0 ? -1 : totalRead;
			else if (read == 0)
				break;
			singleByte[0] = (byte) (singleChar[0] & 0xff);
			totalRead += target.write(singleByte);
		}
		return totalRead;
	}
}
