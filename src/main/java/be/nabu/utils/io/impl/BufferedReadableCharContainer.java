package be.nabu.utils.io.impl;

import java.io.IOException;

import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.CharContainer;
import be.nabu.utils.io.api.ReadableCharContainer;

public class BufferedReadableCharContainer implements ReadableCharContainer {

	private EOFReadableCharContainer parent;
	private CharContainer buffer;
	private int bufferSize;
	private boolean closed = false;
	
	public BufferedReadableCharContainer(ReadableCharContainer parent, int bufferSize) {
		this.parent = new EOFReadableCharContainer(parent);
		this.buffer = IOUtils.newCharBuffer(bufferSize);
		this.bufferSize = bufferSize;
	}
	
	@Override
	public void close() throws IOException {
		parent.close();
	}

	@Override
	public int read(char[] chars) {
		return read(chars, 0, chars.length);
	}

	@Override
	public int read(char[] chars, int offset, int length) {
		int totalRead = 0;
		while (length > 0) {
			int read = buffer.read(chars, offset, length);
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
