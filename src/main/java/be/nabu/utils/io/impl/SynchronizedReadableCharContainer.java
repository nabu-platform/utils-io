package be.nabu.utils.io.impl;

import java.io.IOException;
import java.util.concurrent.locks.Lock;

import be.nabu.utils.io.api.IORuntimeException;
import be.nabu.utils.io.api.ReadableCharContainer;

public class SynchronizedReadableCharContainer implements ReadableCharContainer {

	private ReadableCharContainer original;
	private Lock lock;
	
	public SynchronizedReadableCharContainer(ReadableCharContainer original, Lock lock) {
		this.original = original;
		this.lock = lock;
	}
	
	@Override
	public void close() throws IOException {
		try {
			lock.lockInterruptibly();
			try {
				original.close();
			}
			finally {
				lock.unlock();
			}
		}
		catch (InterruptedException e) {
			throw new IOException(e);
		}
	}

	@Override
	public int read(char[] chars) {
		try {
			lock.lockInterruptibly();
			try {
				return original.read(chars);
			}
			finally {
				lock.unlock();
			}
		}
		catch (InterruptedException e) {
			throw new IORuntimeException(e);
		}	
	}

	@Override
	public int read(char[] chars, int offset, int length) {
		try {
			lock.lockInterruptibly();
			try {
				return original.read(chars, offset, length);
			}
			finally {
				lock.unlock();
			}
		}
		catch (InterruptedException e) {
			throw new IORuntimeException(e);
		}
	}

}
