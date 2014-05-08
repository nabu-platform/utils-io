package be.nabu.utils.io.impl;

import java.io.IOException;
import java.io.Reader;
import java.util.Date;

import be.nabu.utils.io.api.ReadableCharContainer;

public class ReadableCharContainerReader extends Reader {

	private ReadableCharContainer container;
	
	/**
	 * The timeout that the reader waits for more data until it returns -1 signaling the end
	 */
	private long timeout = 0;
	
	private boolean closed = false;
	
	public ReadableCharContainerReader(ReadableCharContainer container) {
		this.container = container;
	}
	
	@Override
	public void close() throws IOException {
		closed = true;
		container.close();
	}

	@Override
	public int read(char[] characters, int offset, int length) throws IOException {
		if (closed)
			return -1;
			
		int read = 0;
		Date startedRead = timeout == 0 ? null : new Date();
		while (read == 0) {
			read = container.read(characters, offset, length);
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

}
