package be.nabu.utils.io.containers;

import java.io.IOException;
import java.util.Arrays;

import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.Buffer;
import be.nabu.utils.io.api.ReadableContainer;

public class ReadableContainerChainer<T extends Buffer<T>> implements ReadableContainer<T> {

	private ReadableContainer<T> [] sources;
	private int active = 0;
	
	public ReadableContainerChainer(ReadableContainer<T>...sources) {
		this.sources = sources;
	}
	
	@Override
	public void close() throws IOException {
		IOUtils.close(Arrays.copyOfRange(sources, active, sources.length));
	}

	@Override
	public long read(T target) throws IOException {
		long totalRead = 0;
		// no more data
		if (active >= sources.length) {
			if (totalRead == 0)
				totalRead = -1;
		}
		else {
			long read = sources[active].read(target);
			if (read <= 0) {
				sources[active].close();
				active++;
				return read(target);
			}
			else
				totalRead += read;
		}
		return totalRead;
	}
	
}
