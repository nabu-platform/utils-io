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
import java.io.InputStream;

import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.LimitedReadableContainer;
import be.nabu.utils.io.api.MarkableContainer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.api.ResettableContainer;
import be.nabu.utils.io.api.SkippableContainer;

public class ByteContainerInputStream extends InputStream {
	
	private ReadableContainer<ByteBuffer> container;
	private boolean closed = false;
	private byte [] single = new byte[1];
	private boolean closeIfEmpty;
	
	public ByteContainerInputStream(ReadableContainer<ByteBuffer> container, boolean closeIfEmpty) {
		this.container = container;
		this.closeIfEmpty = closeIfEmpty;
	}
	
	@Override
	public void close() throws IOException {
		closed = true;
		container.close();
	}

	@Override
	public int read(byte [] bytes) throws IOException {
		return read(bytes, 0, bytes.length);
	}
	
	@Override
	public int read() throws IOException {
		int read = read(single);
		if (read == -1)
			return read;
		else
			return single[0] & 0xff;
	}
	
	@Override
	public int read(byte[] bytes, int offset, int length) throws IOException {
		if (closed)
			return -1;
		
		int read = 0;
		// block until data is present
		while (read == 0) {
			read = (int) container.read(IOUtils.wrap(bytes, offset, length, false));
			if (read == 0 && closeIfEmpty) {
				close();
				read = -1;
				break;
			}
		}
		return read;
	}

	@Override
	public long skip(long amount) throws IOException {
		if (container instanceof SkippableContainer)
			return ((SkippableContainer<ByteBuffer>) container).skip(amount);
		else
			return super.skip(amount);
	}

	@Override
	public int available() throws IOException {
		if (container instanceof LimitedReadableContainer)
			return (int) ((LimitedReadableContainer<ByteBuffer>) container).remainingData();
		else
			return super.available();
	}

	@Override
	public synchronized void mark(int readlimit) {
		if (container instanceof MarkableContainer)
			((MarkableContainer<ByteBuffer>) container).mark();
		else
			super.mark(readlimit);
	}

	@Override
	public boolean markSupported() {
		return container instanceof MarkableContainer;
	}

	@Override
	public synchronized void reset() throws IOException {
		if (container instanceof ResettableContainer)
			((ResettableContainer<ByteBuffer>) container).reset();
		else
			super.reset();
	}

}
