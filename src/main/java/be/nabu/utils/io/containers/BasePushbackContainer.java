package be.nabu.utils.io.containers;

import java.io.IOException;

import be.nabu.utils.io.api.Buffer;
import be.nabu.utils.io.api.PushbackContainer;

abstract public class BasePushbackContainer<T extends Buffer<T>> implements PushbackContainer<T> {
	
	private T buffer;
	
	private boolean fifo = false;
	
	
	protected T getBuffer() {
		return buffer;
	}
	
	@Override
	public void pushback(T data) throws IOException {
		if (buffer == null) {
			buffer = data.getFactory().newInstance();
		}
		// if we follow the first in, first out principle, any new push back data should come after the already pushed back data
		if (fifo || buffer.remainingData() == 0) {
			buffer.write(data);
		}
		// otherwise, any newly written data has to be read before old pushed back
		else {
			T newBuffer = data.getFactory().newInstance();
			newBuffer.write(data);
			newBuffer.write(buffer);
			buffer = newBuffer;
		}
	}

	public long getBufferSize() {
		return buffer == null ? 0 : buffer.remainingData();
	}
}
