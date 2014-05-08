package be.nabu.utils.io.impl;

import java.io.IOException;
import java.util.Arrays;

import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ReadableByteContainer;

public class ReadableByteContainerChainer implements ReadableByteContainer {

	private ReadableByteContainer [] sources;
	private int active = 0;
	
	public ReadableByteContainerChainer(ReadableByteContainer...sources) {
		this.sources = sources;
	}
	
	@Override
	public void close() throws IOException {
		IOUtils.close(Arrays.copyOfRange(sources, active, sources.length));
	}

	@Override
	public int read(byte[] bytes) {
		return read(bytes, 0, bytes.length);
	}

	@Override
	public int read(byte[] bytes, int offset, int length) {
		// no more data
		if (active >= sources.length)
			return -1;
		int read = sources[active].read(bytes, offset, length);
		if (read <= 0) {
			IOUtils.close(sources[active]);
			active++;
			read = read(bytes, offset, length);
		}
		return read;
	}
	
}
