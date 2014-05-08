package be.nabu.utils.io.containers.chars;

import java.io.IOException;
import java.io.Reader;

import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.CharBuffer;
import be.nabu.utils.io.api.ReadableContainer;

public class CharContainerReader extends Reader {

	private ReadableContainer<CharBuffer> container;
	
	private boolean closed = false;
	
	public CharContainerReader(ReadableContainer<CharBuffer> container) {
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
		while (length > 0 && read == 0)
			read = (int) container.read(IOUtils.wrap(characters, offset, length, false));
		
		return read;
	}

}
