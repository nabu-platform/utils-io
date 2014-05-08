package be.nabu.utils.io.impl;

import java.io.IOException;

import be.nabu.utils.io.api.LimitedReadableByteContainer;
import be.nabu.utils.io.api.ReadableByteContainer;

public class FixedLengthReadableByteContainer implements LimitedReadableByteContainer {

	private long fixedLength;
	private long alreadyRead = 0;
	private ReadableByteContainer parent;
	private boolean closed = false;
	
	public FixedLengthReadableByteContainer(ReadableByteContainer parent, long fixedLength) {
		this.parent = parent;
		this.fixedLength = fixedLength;
	}
	
	@Override
	public void close() throws IOException {
		closed = true;
		parent.close();
	}

	@Override
	public int read(byte[] bytes) {
		return read(bytes, 0, bytes.length);
	}

	@Override
	public int read(byte[] bytes, int offset, int length) {
		if (closed)
			return -1;
		// read nothing more
		else if (alreadyRead >= fixedLength)
			return 0;
		
		length = (int) Math.min(length, fixedLength - alreadyRead);
		int read = parent.read(bytes, offset, length);
		if (read == -1) {
			closed = true;
			return -1;
		}
		alreadyRead += read;
		return read;
	}

	@Override
	public long remainingData() {
		return fixedLength - alreadyRead;
	}

}
