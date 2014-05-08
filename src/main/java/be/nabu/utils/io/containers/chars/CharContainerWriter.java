package be.nabu.utils.io.containers.chars;

import java.io.IOException;
import java.io.Writer;

import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.CharBuffer;
import be.nabu.utils.io.api.WritableContainer;

public class CharContainerWriter extends Writer {

	private WritableContainer<CharBuffer> container;
	
	public CharContainerWriter(WritableContainer<CharBuffer> container) {
		this.container = container;
	}
	
	@Override
	public void close() throws IOException {
		container.close();
	}

	@Override
	public void flush() throws IOException {
		container.flush();
	}

	@Override
	public void write(char[] characters, int offset, int length) throws IOException {
		while (length > 0)
			length -= container.write(IOUtils.wrap(characters, offset, length, true));
	}

}
