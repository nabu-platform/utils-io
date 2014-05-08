package be.nabu.utils.io.impl;

import java.io.IOException;

import be.nabu.utils.io.api.ReadableByteContainer;

/**
 * Allows you to specify a minimum amount that has to be read
 * The container will block until then!
 */
public class MinimalReadableByteContainer implements ReadableByteContainer {

	private ReadableByteContainer parent;
	private long alreadyRead, minimumAmountToRead;
	
	public MinimalReadableByteContainer(ReadableByteContainer parent, long minimumAmountToRead) {
		this.parent = parent;
		this.minimumAmountToRead = minimumAmountToRead;
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
		// if we still need to read something and we haven't done so already, do so
		while (length > 0) {
			int read = parent.read(bytes, offset, length);
			if (read == -1)
				return totalRead == 0 ? -1 : totalRead;
			else if (read == 0 && alreadyRead >= minimumAmountToRead)
				break;
			totalRead += read;
			alreadyRead += read;
			offset += read;
			length -= read;
		}
		return totalRead;
	}

}
