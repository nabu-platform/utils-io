package be.nabu.utils.io.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import be.nabu.utils.io.api.ReadableByteContainer;

public class ReadableByteContainerInputStream extends InputStream {

	private ReadableByteContainer container;
	private byte [] buffer = new byte[1];
	
	/**
	 * The timeout that the reader waits for more data until it returns -1 signaling the end
	 */
	private long timeout = 0;
	
	private boolean closed = false;
	
	public ReadableByteContainerInputStream(ReadableByteContainer container) {
		this.container = container;
	}
	
	@Override
	public int read() throws IOException {
		int read = read(buffer);
		if (read == -1)
			return read;
		else
			return buffer[0] & 0xff;
	}

	@Override
	public void close() throws IOException {
		closed = true;
		container.close();
	}

	@Override
	public int read(byte[] bytes, int offset, int length) throws IOException {
		if (closed)
			return -1;
		
		int read = 0;
		Date startedRead = timeout == 0 ? null : new Date();
		// block until data is present
		while (read == 0) {
			read = container.read(bytes, offset, length);
			// if there is no data, check if we should time out
			if (read == 0) {
				if (timeout == 0) {
					close();
					return -1;
				}
				else if (new Date().getTime() - startedRead.getTime() > timeout) {
					close();
					return -1;
				}
			}
		}
		return read;
	}

	@Override
	public int read(byte [] bytes) throws IOException {
		return read(bytes, 0, bytes.length);
	}
	
}