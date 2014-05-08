package be.nabu.utils.io.containers;

import java.io.IOException;
import java.util.concurrent.locks.Lock;

import be.nabu.utils.io.api.Buffer;
import be.nabu.utils.io.api.WritableContainer;

public class SynchronizedWritableContainer<T extends Buffer<T>> implements WritableContainer<T> {

	private WritableContainer<T> original;
	private Lock lock;
	
	public SynchronizedWritableContainer(WritableContainer<T> original, Lock lock) {
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
	public long write(T target) throws IOException {
		try {
			lock.lockInterruptibly();
			try {
				return original.write(target);
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
	public void flush() throws IOException {
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
			throw new IOException(e);
		}
	}
}
