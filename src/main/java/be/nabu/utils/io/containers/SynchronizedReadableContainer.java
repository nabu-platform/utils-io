package be.nabu.utils.io.containers;

import java.io.IOException;
import java.util.concurrent.locks.Lock;

import be.nabu.utils.io.api.Buffer;
import be.nabu.utils.io.api.ReadableContainer;

public class SynchronizedReadableContainer<T extends Buffer<T>> implements ReadableContainer<T> {

	private ReadableContainer<T> original;
	private Lock lock;
	
	public SynchronizedReadableContainer(ReadableContainer<T> original, Lock lock) {
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
	public long read(T target) throws IOException {
		try {
			lock.lockInterruptibly();
			try {
				return original.read(target);
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
