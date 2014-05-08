package be.nabu.utils.io.containers;

import java.io.IOException;

import be.nabu.utils.io.api.Buffer;
import be.nabu.utils.io.api.WritableContainer;

public class MinimalWritableContainer<T extends Buffer<T>> implements WritableContainer<T> {
	
	private WritableContainer<T> parent;
	private long alreadyWritten, minimumAmountToWrite;
	
	public MinimalWritableContainer(WritableContainer<T> parent, long minimumAmountToWrite) {
		this.parent = parent;
		this.minimumAmountToWrite = minimumAmountToWrite;
	}
	
	@Override
	public void close() throws IOException {
		parent.close();
	}

	@Override
	public long write(T source) throws IOException {
		int totalWritten = 0;
		// if we still need to read something and we haven't done so already, do so
		while (source.remainingData() > 0) {
			long write = parent.write(source);
			if (write == -1)
				return totalWritten == 0 ? -1 : totalWritten;
			else if (write == 0 && alreadyWritten >= minimumAmountToWrite)
				break;
			totalWritten += write;
			alreadyWritten += write;
		}
		return totalWritten;
	}

	@Override
	public void flush() throws IOException {
		parent.flush();
	}
}
