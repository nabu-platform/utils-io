package be.nabu.utils.io.impl;

import java.io.IOException;

import be.nabu.utils.io.api.WritableByteContainer;

/**
 * Allows you to specify a minimum amount that has to be read
 * The container will block until then!
 */
public class MinimalWritableByteContainer implements WritableByteContainer {

	private WritableByteContainer parent;
	private long alreadyWritten, minimumAmountToWrite;
	
	public MinimalWritableByteContainer(WritableByteContainer parent, long minimumAmountToWrite) {
		this.parent = parent;
		this.minimumAmountToWrite = minimumAmountToWrite;
	}
	
	@Override
	public void close() throws IOException {
		parent.close();
	}

	@Override
	public int write(byte[] bytes) {
		return write(bytes, 0, bytes.length);
	}

	@Override
	public int write(byte[] bytes, int offset, int length) {
		int totalWritten = 0;
		// if we still need to read something and we haven't done so already, do so
		while (length > 0) {
			int write = parent.write(bytes, offset, length);
			if (write == -1)
				return totalWritten == 0 ? -1 : totalWritten;
			else if (write == 0 && alreadyWritten >= minimumAmountToWrite)
				break;
			totalWritten += write;
			alreadyWritten += write;
			offset += write;
			length -= write;
		}
		return totalWritten;
	}

	@Override
	public void flush() {
		parent.flush();
	}

}
