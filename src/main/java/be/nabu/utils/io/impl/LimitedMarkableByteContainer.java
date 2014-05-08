package be.nabu.utils.io.impl;

import java.io.IOException;

import be.nabu.utils.io.api.MarkableByteContainer;
import be.nabu.utils.io.api.ReadableByteContainer;

public class LimitedMarkableByteContainer implements MarkableByteContainer {

	private DynamicByteContainer backingContainer;
	private ReadableByteContainer parent;
	private boolean reset = false;
	
	/**
	 * If the limit is 0 or smaller, there is no limit
	 */
	private long readLimit;
	
	public LimitedMarkableByteContainer(ReadableByteContainer parent, long readLimit) {
		this.parent = parent;
		this.readLimit = readLimit;
	}
	
	@Override
	public void reset() {
		if (backingContainer == null)
			throw new IllegalStateException("No mark has been set or the readLimit has been exceeded, can not reset");
		reset = true;
		backingContainer.reset();
	}

	@Override
	public int read(byte[] bytes) {
		return read(bytes, 0, bytes.length);
	}

	@Override
	public int read(byte[] bytes, int offset, int length) {
		int totalRead = 0;
		while (length > 0) {
			int read = 0;
			// if we have reset the data and there is still some in the backing container, read it
			if (reset && backingContainer.remainingData() > 0)
				read = backingContainer.read(bytes, offset, length);
			// if we have not reset it or there is no more data in the backing container, read from the parent
			if (!reset || read == 0) {
				// if we are reading from parent again, undo the reset
				reset = false;
				read = parent.read(bytes, offset, length);
				if (read == -1)
					return totalRead == 0 ? -1 : totalRead;
				// write it to the backing container so it is buffered for reset
				if (backingContainer != null) {
					// if we have stored more data than the given readLimit we should clear all the data
					if (readLimit > 0 && backingContainer.size() >= readLimit)
						backingContainer = null;
					else
						backingContainer.write(bytes, offset,  readLimit == 0 ? read : (int) Math.min(read, readLimit - backingContainer.size()));
				}
			}
			if (read == 0)
				break;
			else {
				length -= read;
				offset += read;
				totalRead += read;
			}
		}
		return totalRead;
	}

	@Override
	public void close() throws IOException {
		if (backingContainer != null)
			backingContainer.close();
		parent.close();
	}

	@Override
	public void mark() {
		if (backingContainer != null)
			unmark();
		backingContainer = new DynamicByteContainer();
		backingContainer.mark();
	}

	@Override
	public void unmark() {
		if (backingContainer != null) {
			backingContainer.unmark();
			backingContainer = null;
			reset = false;
		}
	}

}
