package be.nabu.utils.io.containers;

import java.io.IOException;

import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.Buffer;
import be.nabu.utils.io.api.ReadableContainer;

public class ReadableContainerChainer<T extends Buffer<T>> implements ReadableContainer<T> {

	private ReadableContainer<T> [] sources;
	private int active = 0;
	private boolean closeIfRead = true;
	
	public ReadableContainerChainer(boolean closeIfRead, ReadableContainer<T>...sources) {
		this.closeIfRead = closeIfRead;
		this.sources = sources;
	}
	
	@Override
	public void close() throws IOException {
		IOUtils.close(sources);
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
				if (closeIfRead)
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
