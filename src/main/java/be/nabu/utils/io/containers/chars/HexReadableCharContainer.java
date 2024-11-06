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

package be.nabu.utils.io.containers.chars;

import java.io.IOException;

import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.CharBuffer;
import be.nabu.utils.io.api.ReadableContainer;

public class HexReadableCharContainer implements ReadableContainer<CharBuffer> {

	private ReadableContainer<ByteBuffer> bytes;
	private ByteBuffer buffer = IOUtils.newByteBuffer();
	private byte[] singleByte = new byte[1];
	private boolean closed = false;
	
	public HexReadableCharContainer(ReadableContainer<ByteBuffer> bytes) {
		this.bytes = bytes;
	}
	
	@Override
	public void close() throws IOException {
		this.closed = true;
		bytes.close();
	}

	@Override
	public long read(CharBuffer target) throws IOException {
		long totalRead = 0;
		while (target.remainingSpace() > 0) {
			if (buffer.remainingData() == 0) {
				long read = bytes.read(buffer);
				if (read == 0)
					break;
				else if (read == -1) {
					closed = true;
					break;
				}
			}
			if (buffer.remainingData() > 0) {
				if (buffer.read(singleByte) != 1)
					throw new IOException("Could not read from temporary buffer");
				String formatted = String.format("%02x", singleByte[0] & 0xff);
				if (target.write(formatted.toCharArray()) != formatted.length())
					throw new IOException("Could not write to target buffer");
				totalRead += formatted.length();
			}
		}
		return totalRead == 0 && closed ? -1 : totalRead;
	}
	
}
