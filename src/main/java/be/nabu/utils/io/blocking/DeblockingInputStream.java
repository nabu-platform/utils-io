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

package be.nabu.utils.io.blocking;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.buffers.bytes.ByteBufferFactory;

public class DeblockingInputStream implements AutoCloseable {
	
	private ByteBuffer deblockingBuffer;
	private InputStream input;
	private Thread thread;
	private List<Thread> ioThreads = new ArrayList<Thread>();
	private boolean closed;
	
	public DeblockingInputStream(InputStream input) {
		this.input = input;
		deblockingBuffer = ByteBufferFactory.getInstance().newInstance(1024 * 100, true);
		start();
	}
	
	private void start() {
		// this reads the io
		thread = new Thread(new Runnable() {
			@Override
			public void run() {
				int read = 0;
				byte [] buffer = new byte[1024];
				try {
					// the read will block until available
					while ((read = input.read(buffer)) >= 0) {
						// we continue writing until it is empty
						while (read > 0) {
							read -= deblockingBuffer.write(buffer, 0, read);
							// we just wrote shizzle, wake up the last thread to register
							if (!ioThreads.isEmpty()) {
								ioThreads.get(ioThreads.size() - 1).interrupt();
							}
							if (read > 0) {
								try {
									Thread.sleep(5000);
								}
								catch (InterruptedException e) {
									// ignore, we continue
									// clear flag?
									Thread.interrupted();
								}
							}
						}
					}
					closed = true;
				}
				catch (IOException e) {
					try {
						close();
					}
					catch (Exception e1) {
						// do nothing
					}
				}
				finally {
					// interrupt them all to stop them!
					while (!ioThreads.isEmpty()) {
						ioThreads.remove(ioThreads.size() - 1).interrupt();
					}
				}
			}
		});
		thread.setDaemon(true);
		thread.setName("deblocking-inputstream");
		thread.start();
	}
	
	public InputStream newInputStream() {
		return new InputStream() {
			private boolean closed;
			private byte [] single = new byte[1];
			@Override
			public int available() throws IOException {
				return (int) (isClosed() ? 0 : deblockingBuffer.remainingData());
			}
			@Override
			public void close() throws IOException {
				closed = true;
			}
			private boolean isClosed() {
				return closed || DeblockingInputStream.this.closed;
			}
			@Override
			public int read() throws IOException {
				int read = read(single);
				if (read < 0) {
					return read;
				}
				else {
					return single[0] & 0xff;
				}
			}
			@Override
			public int read(byte[] b, int off, int len) throws IOException {
				synchronized (ioThreads) {
					ioThreads.add(Thread.currentThread());
				}
				try {
					int read = 0;
					// we wait as long as we have no data, we need to block
					while (!isClosed() && read == 0) {
						if (available() == 0) {
							try {
								Thread.sleep(5000);
							}
							// interruptible I/O!
							catch (InterruptedException e) {
								// ignore
							}
						}
						read = deblockingBuffer.read(b, off, len);
						// we read some data, go fetch some more!
						thread.interrupt();
						if (read < 0) {
							closed = true;
						}
						if (read != 0) {
							return read;
						}
					}
					// other streams that wrap on top of this one do not always like a 0 :(
					// especially the buffered reader for example
//					else {
//						return 0;
//					}
				}
				finally {
					synchronized (ioThreads) {
						ioThreads.remove(Thread.currentThread());
					}
				}
				return -1;
			}
			@Override
			public int read(byte[] b) throws IOException {
				return read(b, 0, b.length);
			}
		};
	}

	@Override
	public void close() throws Exception {
		closed = true;
		input.close();
	}
}
