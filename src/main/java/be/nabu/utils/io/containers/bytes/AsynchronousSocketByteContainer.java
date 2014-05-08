package be.nabu.utils.io.containers.bytes;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.Container;
import be.nabu.utils.io.buffers.bytes.DynamicByteBuffer;
import be.nabu.utils.io.buffers.bytes.NioByteBufferWrapper;

/**
 * This can only be compiled with java 1.7+
 */
public class AsynchronousSocketByteContainer implements Container<be.nabu.utils.io.api.ByteBuffer> {

	private Future<Void> connect;
	private Future<Integer> read, write;
	private AsynchronousSocketChannel socket;
	private AsynchronousChannelGroup group;
	private ByteBuffer buffer;
	private DynamicByteBuffer writeBuffer = new DynamicByteBuffer();
	private boolean readyForReading = false;
	
	public AsynchronousSocketByteContainer(AsynchronousChannelGroup group, AsynchronousSocketChannel socket, int bufferSize) {
		this.group = group;
		this.socket = socket;
		this.buffer = ByteBuffer.allocate(bufferSize);
	}
	
	public AsynchronousSocketByteContainer(SocketAddress remote) throws IOException {
		this(null, null, remote, 1024 * 10);
	}
	
	public AsynchronousSocketByteContainer(AsynchronousChannelGroup group, SocketAddress local, SocketAddress remote, int bufferSize) throws IOException {
		this.group = group;
		socket = AsynchronousSocketChannel.open(group);
		if (local != null)
			socket.bind(local);
		connect = socket.connect(remote);
		buffer = ByteBuffer.allocate(bufferSize);
	}

	@Override
	public long read(be.nabu.utils.io.api.ByteBuffer source) throws IOException {
		// the connect has to be done
		if (connect != null && !connect.isDone())
			return 0;
		int totalRead = 0;
		while (source.remainingSpace() > 0) {
			// stop reading if one is busy but not ready
			if (read != null && !read.isDone())
				break;
			
			// if the async read was done but we haven't flipped it yet, do so
			if (read != null && read.isDone() && !readyForReading) {
				try {
					int amountRead = read.get();
					if (amountRead == -1)
						return totalRead == 0 ? -1 : totalRead;
					else if (amountRead == 0)
						return totalRead == 0 ? -1 : totalRead; 
				}
				catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				catch (ExecutionException e) {
					throw new RuntimeException(e);
				}
				buffer.flip();
				readyForReading = true;
			}
			if (read != null && read.isDone() && buffer.hasRemaining()) {
				int amount = (int) Math.min(source.remainingData(), buffer.remaining());
				source.write(new NioByteBufferWrapper(buffer, true));
				totalRead += amount;
			}
			else {
				buffer.clear();
				readyForReading = false;
				this.read = socket.read(buffer);
			}
		}
		return totalRead == 0 && !socket.isOpen() ? -1 : totalRead;
	}

	@Override
	public void close() throws IOException {
		flush();
		socket.close();
	}

	@Override
	public long write(be.nabu.utils.io.api.ByteBuffer target) throws IOException {
		long length = target.remainingData();
		// the connect has to be done
		if (connect != null && !connect.isDone())
			return 0;
		// if we have a write pending, stop
		if (write != null && !write.isDone())
			return 0;
		// or if a write was done, remove from the buffer what was written
		else if (write != null && write.isDone()) {
			try {
				int amountWritten = write.get();
				// if nothing was written, stop
				if (amountWritten == 0)
					return 0;
				// otherwise clear the writeBuffer from what has been written already
				else
					writeBuffer.skip(amountWritten);
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			catch (ExecutionException e) {
				throw new RuntimeException(e);
			}
		}
		// if we get here, write data to the buffer and initiate a write
		writeBuffer.write(target);
		this.write = socket.write(ByteBuffer.wrap(IOUtils.peekBytes(writeBuffer)));
//		writeBuffer.write(bytes, offset, length);
		return length;
	}

	@Override
	public void flush() throws IOException {
		// if there is data in the buffer apparently the write is already done, flush the remaining data
		if (write != null && write.isDone() && writeBuffer.remainingData() > 0) {
			try {
				int amountWritten = write.get();
				if (amountWritten > 0)
					writeBuffer.skip(amountWritten);
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			catch (ExecutionException e) {
				throw new RuntimeException(e);
			}
		}
		if (writeBuffer.remainingData() > 0) {
			byte [] dataToWrite = new byte[(int) writeBuffer.remainingData()];
			write = socket.write(ByteBuffer.wrap(dataToWrite));
		}
	}

	public AsynchronousChannelGroup getGroup() {
		return group;
	}

	public AsynchronousSocketChannel getSocket() {
		return socket;
	}
}
