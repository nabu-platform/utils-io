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
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.Container;

public class ByteChannelContainer<T extends ByteChannel> implements Container<be.nabu.utils.io.api.ByteBuffer> {
	
	private T channel;
	private byte [] bytes = new byte[4096];
	private boolean isClosed;

	public ByteChannelContainer(T channel) {
		this.channel = channel;
	}

	public T getChannel() {
		return channel;
	}

	@Override
	public long read(be.nabu.utils.io.api.ByteBuffer target) throws IOException {
		if (isClosed()) {
			return -1;
		}
		else if (!isReady()) {
			return 0;
		}
		long totalRead = 0;
		while (!isClosed && target.remainingSpace() > 0) {
			// there is an edge case in large volumes that sometimes a socket keeps getting activated by a READ signal but never stops
			// this is usually a case of an EOF not being interpreted correctly but there does not appear to be an EOF problem up the stack (except perhaps a problem in the SSLEngine with a package larger than package size)
			// before the check was specifically for -1, now it's been broadened to anything negative (see comments below)
			int read = channel.read(ByteBuffer.wrap(bytes, 0, (int) Math.min(bytes.length, target.remainingSpace())));
			/*
			 * EOF = -1;              // End of file
				UNAVAILABLE = -2;      // Nothing available (non-blocking)
				INTERRUPTED = -3;      // System call interrupted
				UNSUPPORTED = -4;      // Operation not supported
				THROWN = -5;           // Exception thrown in JNI code
				UNSUPPORTED_CASE = -6; // This case not supported
				
				judging from the source, "unavailable" is always converted into a 0, interrupted is only sent back _if_ the channel is no longer isOpen()
				in theory the others could be returned at any point, especially the THROWN looks suspicious
			 */
			if (read < 0) {
				isClosed = true;
				break;
			}
			else if (read == 0)
				break;
			else
				totalRead += read;
			if (target.write(bytes, 0, read) != read)
				throw new IOException("Can not write all data to the buffer");
		}
		isClosed |= !channel.isOpen();
		return totalRead == 0 && isClosed() ? -1 : totalRead;
	}

	@Override
	public void close() throws IOException {
		isClosed = true;
		channel.close();
	}

	@Override
	public long write(be.nabu.utils.io.api.ByteBuffer source) throws IOException {
		if (isClosed()) {
			return -1;
		}
		else if (!isReady()) {
			return 0;
		}
		long totalWritten = 0;
		while (!isClosed && source.remainingData() > 0) {
			int read = (int) source.peek(IOUtils.wrap(bytes, false));
			int written = channel.write(ByteBuffer.wrap(bytes, 0, read));
			// skip the data that was successfully written
			source.skip(written);
			totalWritten += written;
			if (written != read) {
				break;
			}
		}
		return totalWritten == 0 && isClosed() ? -1 : totalWritten;
	}

	@Override
	public void flush() {
		// do nothing
	}
	
	protected boolean isReady() throws IOException {
		return channel.isOpen();
	}

	boolean isClosed() {
		return isClosed;
	}

	void setClosed(boolean isClosed) {
		this.isClosed = isClosed;
	}
}
