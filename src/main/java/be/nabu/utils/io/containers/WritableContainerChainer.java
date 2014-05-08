package be.nabu.utils.io.containers;

import java.io.IOException;

import be.nabu.utils.io.api.Buffer;
import be.nabu.utils.io.api.LimitedWritableContainer;
import be.nabu.utils.io.api.WritableContainer;

public class WritableContainerChainer<T extends Buffer<T>> implements WritableContainer<T> {

	private LimitedWritableContainer<T> [] targets;
	private int current = 0;
	private boolean closed = false;
	
	public WritableContainerChainer(LimitedWritableContainer<T>...targets) {
		this.targets = targets;
	}
	
	@Override
	public void close() throws IOException {
		closed = true;
		IOException exception = null;
		for (int i = current; i < targets.length; i++) {
			try {
				targets[i].close();
			}
			catch (IOException e) {
				exception = e;
			}
		}
		if (exception != null)
			throw exception;
	}

	@Override
	public long write(T buffer) throws IOException {
		long totalWritten = 0;
		while (buffer.remainingData() > 0) {
			if (current >= targets.length)
				return totalWritten == 0 && closed ? -1 : totalWritten;
			
			if (targets[current].remainingSpace() == 0) {
				targets[current].close();
				current++;
			}
			else
				totalWritten += targets[current].write(buffer);
		}
		return totalWritten;
	}

	@Override
	public void flush() throws IOException {
		if (current < targets.length)
			targets[current].flush();
	}

}
