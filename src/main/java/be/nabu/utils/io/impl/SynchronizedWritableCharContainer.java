package be.nabu.utils.io.impl;

import java.io.IOException;
import java.util.concurrent.locks.Lock;

import be.nabu.utils.io.api.IORuntimeException;
import be.nabu.utils.io.api.WritableCharContainer;

public class SynchronizedWritableCharContainer implements WritableCharContainer {

	private WritableCharContainer original;
	private Lock lock;
	
	public SynchronizedWritableCharContainer(WritableCharContainer original, Lock lock) {
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
	public int write(char[] chars) {
		try {
			lock.lockInterruptibly();
			try {
				return original.write(chars);
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
	public int write(char[] chars, int offset, int length) {
		try {
			lock.lockInterruptibly();
			try {
				return original.write(chars, offset, length);
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
