package be.nabu.utils.io.impl;

import java.io.IOException;
import java.util.concurrent.locks.Lock;

import be.nabu.utils.io.api.IORuntimeException;
import be.nabu.utils.io.api.ReadableByteContainer;

public class SynchronizedReadableByteContainer implements ReadableByteContainer {

	private ReadableByteContainer original;
	private Lock lock;
	
	public SynchronizedReadableByteContainer(ReadableByteContainer original, Lock lock) {
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
	public int read(byte[] bytes) {
		try {
			lock.lockInterruptibly();
			try {
				return original.read(bytes);
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
	public int read(byte[] bytes, int offset, int length) {
		try {
			lock.lockInterruptibly();
			try {
				return original.read(bytes, offset, length);
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
