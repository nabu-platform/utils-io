package be.nabu.utils.io.impl;

import java.io.IOException;

import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteContainer;
import be.nabu.utils.io.api.ReadableByteContainer;

public class BufferedReadableByteContainer implements ReadableByteContainer {

	private EOFReadableByteContainer parent;
	private ByteContainer buffer;
	private int bufferSize;
	private boolean closed = false;
	
	public BufferedReadableByteContainer(ReadableByteContainer parent, int bufferSize) {
		this.parent = new EOFReadableByteContainer(parent);
		this.buffer = IOUtils.newByteBuffer(bufferSize);
		this.bufferSize = bufferSize;
	}
	
	@Override
	public void close() throws IOException {
		parent.close();
	}

	@Override
	public int read(byte[] bytes) {
		return read(bytes, 0, bytes.length);
	}

	@Override
	public int read(byte[] bytes, int offset, int length) {
		int totalRead = 0;
		while (length > 0) {
			int read = buffer.read(bytes, offset, length);
			if (read == 0) {
				if (closed)
					return -1;
				long copied = IOUtils.copy(IOUtils.limitReadable(parent, bufferSize), buffer);
				if (copied == 0 && parent.isEOF()) {
					closed = true;
					break;
				}
				// no more data atm
				else if (copied == 0)
					break;
			}
			else {
				offset += read;
				length -= read;
				totalRead += read;
			}
		}
		return totalRead == 0 && closed ? -1 : totalRead;
	}

}
