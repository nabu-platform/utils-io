package be.nabu.utils.io.containers;

import java.io.IOException;

import be.nabu.utils.io.api.Buffer;
import be.nabu.utils.io.api.LimitedReadableContainer;
import be.nabu.utils.io.api.ReadableContainer;

public class FixedLengthReadableContainer<T extends Buffer<T>> implements LimitedReadableContainer<T> {

	private long fixedLength;
	private long alreadyRead = 0;
	private ReadableContainer<T> parent;
	private boolean closed = false;
	
	public FixedLengthReadableContainer(ReadableContainer<T> parent, long fixedLength) {
		this.parent = parent;
		this.fixedLength = fixedLength;
	}
	
	@Override
	public void close() throws IOException {
		closed = true;
		parent.close();
	}

	@Override
	public long read(T target) throws IOException {
		if (closed || alreadyRead >= fixedLength)
			return -1;
		
		long amount = Math.min(target.remainingSpace(), fixedLength - alreadyRead);
		long read = 0;
		if (amount < target.remainingSpace()) {
			T temporary = target.getFactory().newInstance(amount, false);
			read = parent.read(temporary);
			if (read > 0)
				temporary.write(target);
		}
		else
			read = parent.read(target);
		
		if (read == -1)
			closed = true;
		else
			alreadyRead += read;
		
		return read;
	}

	@Override
	public long remainingData() {
		return fixedLength - alreadyRead;
	}

}
