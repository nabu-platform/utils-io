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
import java.security.MessageDigest;

import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.Container;
import be.nabu.utils.io.api.WritableContainer;

public class ByteContainerDigest implements Container<ByteBuffer> {

	private ByteBuffer result;
	private ByteBuffer buffer = IOUtils.newByteBuffer();
	private byte [] bytes = new byte[4096];
	private boolean closed = false;
	
	private WritableContainer<ByteBuffer> chainedOutput;
	private MessageDigest digest;
	
	public ByteContainerDigest(WritableContainer<ByteBuffer> chainedOutput, MessageDigest digest) {
		this.digest = digest;
		this.chainedOutput = chainedOutput;
	}

	@Override
	public long read(ByteBuffer target) throws IOException {
		if (result == null)
			return 0;
		else
			return result.read(target);
	}

	@Override
	public void close() throws IOException {
		closed = true;
		try {
			chainedOutput.close();
		}
		finally {
			if (result == null)
				result = IOUtils.wrap(digest.digest(), true);
		}
	}

	@Override
	public long write(ByteBuffer source) throws IOException {
		long totalWritten = 0;
		while (IOUtils.copyBytes(buffer, chainedOutput) == 0 && source.remainingData() > 0) {
			int read = source.read(bytes, 0, (int) Math.min(bytes.length, source.remainingData()));
			digest.update(bytes, 0, read);
			int written = (int) chainedOutput.write(IOUtils.wrap(bytes, 0, read, true));
			if (written == -1) {
				closed = true;
				break;
			}
			if (written != read)
				buffer.write(bytes, written, read - written);
			totalWritten += written;
		}
		return totalWritten == 0 && closed ? -1 : totalWritten;
	}

	@Override
	public void flush() throws IOException {
		chainedOutput.flush();
	}
}
