package be.nabu.utils.io.impl;

import java.io.IOException;
import java.util.concurrent.locks.Lock;

import be.nabu.utils.io.api.IORuntimeException;
import be.nabu.utils.io.api.WritableByteContainer;

public class SynchronizedWritableByteContainer implements WritableByteContainer {

	private WritableByteContainer original;
	private Lock lock;
	
	public SynchronizedWritableByteContainer(WritableByteContainer original, Lock lock) {
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
	public int write(byte[] bytes) {
		try {
			lock.lockInterruptibly();
			try {
				return original.write(bytes);
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
	public int write(byte[] bytes, int offset, int length) {
		try {
			lock.lockInterruptibly();
			try {
				return original.write(bytes, offset, length);
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
	public void flush() {
		try {
			lock.lockInterruptibly();
			try {
				original.flush();
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
