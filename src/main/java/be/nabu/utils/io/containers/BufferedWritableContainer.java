package be.nabu.utils.io.containers;

import java.io.IOException;

import be.nabu.utils.io.api.Buffer;
import be.nabu.utils.io.api.WritableContainer;

public class BufferedWritableContainer<T extends Buffer<T>> implements WritableContainer<T> {

	private WritableContainer<T> parent;
	private T buffer;
	private boolean closed = false;
	private boolean allowPartialFlush = false;
	
	public BufferedWritableContainer(WritableContainer<T> parent, T buffer) {
		this.parent = parent;
		this.buffer = buffer;
	}
	
	@Override
	public void close() throws IOException {
		flush();
		parent.close();
		closed = true;
	}

	@Override
	public long write(T buffer) throws IOException {
		int totalWritten = 0;
		while (buffer.remainingData() > 0) {
			if (this.buffer.remainingSpace() == 0) {
				if (parent.write(this.buffer) == -1) {
					closed = true;
					break;
				}
			}
			long written = this.buffer.write(buffer);
			if (written == -1) {
				closed = true;
				break;
			}
			else if (written == 0) 
				break;
			else
				totalWritten += written;
		}
		return totalWritten == 0 && closed ? -1 : totalWritten;
	}

	@Override
	public void flush() throws IOException {
		parent.write(buffer);
		if (buffer.remainingData() > 0 && !allowPartialFlush) {
			throw new IOException("Could not flush the entire buffer to the parent " + parent + " (allow partial: " + allowPartialFlush + "), " + buffer.remainingData() + " remaining");
		}
		parent.flush();
	}

	public boolean isAllowPartialFlush() {
		return allowPartialFlush;
	}

	public void setAllowPartialFlush(boolean allowPartialFlush) {
		this.allowPartialFlush = allowPartialFlush;
	}
	
	public long getActualBufferSize() {
		return buffer.remainingData();
	}
	
	public long getMaxBufferSize() {
		return buffer.remainingData() + buffer.remainingSpace();
	}
	
	public long getRemainingBufferSize() {
		return buffer.remainingSpace();
	}
}
