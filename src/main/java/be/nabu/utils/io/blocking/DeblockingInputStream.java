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
					// we wait as long as we have no data, we need to block
					while (!isClosed() && available() == 0) {
						try {
							Thread.sleep(5000);
						}
						// interruptible I/O!
						catch (InterruptedException e) {
							break;
						}
					}
					if (isClosed()) {
						return -1;
					}
					else if (available() > 0) {
						int read = deblockingBuffer.read(b, off, len);
						// we read some data, go fetch some more!
						thread.interrupt();
						return read;
					}
					else {
						return 0;
					}
				}
				finally {
					synchronized (ioThreads) {
						ioThreads.remove(Thread.currentThread());
					}
				}
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
