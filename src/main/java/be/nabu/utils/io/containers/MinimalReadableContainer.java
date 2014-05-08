package be.nabu.utils.io.containers;

import java.io.IOException;

import be.nabu.utils.io.api.Buffer;
import be.nabu.utils.io.api.ReadableContainer;

/**
 * Allows you to specify a minimum amount that has to be read
 * The container will block until then!
 */
public class MinimalReadableContainer<T extends Buffer<T>> implements ReadableContainer<T> {

	private ReadableContainer<T> parent;
	private long alreadyRead, minimumAmountToRead;
	
	public MinimalReadableContainer(ReadableContainer<T> parent, long minimumAmountToRead) {
		this.parent = parent;
		this.minimumAmountToRead = minimumAmountToRead;
	}
	
	@Override
	public void close() throws IOException {
		parent.close();
	}

	@Override
	public long read(T target) throws IOException {
		int totalRead = 0;
		// if we still need to read something and we haven't done so already, do so
		while (target.remainingSpace() > 0) {
			long read = parent.read(target);
			if (read == -1)
				return totalRead == 0 ? -1 : totalRead;
			else if (read == 0 && alreadyRead >= minimumAmountToRead)
				break;
			totalRead += read;
			alreadyRead += read;
		}
		return totalRead;
	}
}
