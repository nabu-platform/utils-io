package be.nabu.utils.io.impl;

import java.io.IOException;
import java.util.Arrays;

import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ReadableCharContainer;

public class ReadableCharContainerChainer implements ReadableCharContainer {

	private ReadableCharContainer [] sources;
	private int active = 0;
	
	public ReadableCharContainerChainer(ReadableCharContainer...sources) {
		this.sources = sources;
	}
	
	@Override
	public void close() throws IOException {
		IOUtils.close(Arrays.copyOfRange(sources, active, sources.length));
	}

	@Override
	public int read(char[] chars) {
		return read(chars, 0, chars.length);
	}

	@Override
	public int read(char[] chars, int offset, int length) {
		// no more data
		if (active >= sources.length)
			return -1;
		int read = sources[active].read(chars, offset, length);
		if (read <= 0) {
			IOUtils.close(sources[active]);
			active++;
			read = read(chars, offset, length);
		}
		return read;
	}
	
}
