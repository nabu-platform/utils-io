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
import java.io.OutputStream;

import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.WritableContainer;

public class ByteContainerOutputStream extends OutputStream {

	private WritableContainer<ByteBuffer> container;
	private byte [] single = new byte [1];
	private boolean failIfFull;
	
	public ByteContainerOutputStream(WritableContainer<ByteBuffer> container, boolean failIfFull) {
		this.container = container;
		this.failIfFull = failIfFull;
	}
	
	@Override
	public void write(int arg0) throws IOException {
		single[0] = (byte) arg0;
		write(single);
	}

	@Override
	public void close() throws IOException {
		container.close();
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		while (len > 0) {
			long written = container.write(IOUtils.wrap(b, off, len, true));
			if (written < 0) {
				throw new IOException("The target container is closed");
			}
			// there is no way to send back partial success
			// you "could" hang until it works but this might result in everlasting hangs
			else if (written == 0 && failIfFull) {
				throw new IOException("The target container is full");
			}
			len -= written;
		}
	}

	@Override
	public void write(byte[] b) throws IOException {
		write(b, 0, b.length);
	}

	@Override
	public void flush() throws IOException {
		container.flush();
	}
	
}
