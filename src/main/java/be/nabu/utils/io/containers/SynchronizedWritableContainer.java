/*
* Copyright (C) 2014 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

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
