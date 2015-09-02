package be.nabu.utils.io.containers.bytes;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.util.Arrays;

import be.nabu.utils.io.api.Container;

public class ByteChannelContainer<T extends ByteChannel> implements Container<be.nabu.utils.io.api.ByteBuffer> {
	
	private T channel;
	private byte [] bytes = new byte[4096];
	private boolean isClosed;
	private byte [] writeBuffer;
	private boolean bufferOutput = false;
	
	public ByteChannelContainer(T channel) {
		this.channel = channel;
	}

	public T getChannel() {
		return channel;
	}

	@Override
	public synchronized long read(be.nabu.utils.io.api.ByteBuffer target) throws IOException {
		if (isClosed()) {
			return -1;
		}
		else if (!isReady()) {
			return 0;
		}
		long totalRead = 0;
		while (target.remainingSpace() > 0) {
			int read = channel.read(ByteBuffer.wrap(bytes, 0, (int) Math.min(bytes.length, target.remainingSpace())));
			if (read == -1) {
				isClosed = true;
				break;
			}
			else if (read == 0)
				break;
			else
				totalRead += read;
			if (target.write(bytes, 0, read) != read)
				throw new IOException("Can not write all data to the buffer");
		}
		isClosed |= !channel.isOpen();
		return totalRead == 0 && isClosed() ? -1 : totalRead;
	}

	@Override
	public void close() throws IOException {
		isClosed = true;
		channel.close();
	}

	@Override
	public synchronized long write(be.nabu.utils.io.api.ByteBuffer source) throws IOException {
		if (isClosed()) {
			return -1;
		}
		else if (!isReady()) {
			return 0;
		}
		long totalWritten = 0;
		if (writeBuffer != null) {
			int write = channel.write(ByteBuffer.wrap(writeBuffer, 0, writeBuffer.length));
			if (write == -1) {
				channel.close();
				isClosed = true;
				return -1;
			}
			// still data in the buffer and we can't write it out, hold it
			else if (write == 0) {
				return 0;
			}
			// could only write it out partially
			else if (write < writeBuffer.length) {
				writeBuffer = Arrays.copyOfRange(writeBuffer, write, writeBuffer.length);
				return write;
			}
			// fully written out
			else {
				totalWritten += write;
				writeBuffer = null;
			}
		}
		while (source.remainingData() > 0) {
			int read = source.read(bytes, 0, (int) Math.min(bytes.length, source.remainingData()));
			int write = channel.write(ByteBuffer.wrap(bytes, 0, read));
			if (write >= 0 && write != read) {
				if (bufferOutput) {
					writeBuffer = Arrays.copyOfRange(bytes, write, read);
					totalWritten += write;
					break;
				}
				else {
					throw new IOException("Could not push all the data to the channel: " + write + "/" + read + " (remaining: " + source.remainingData() + ")");
				}
			}
			totalWritten += read;
		}
		return totalWritten;
	}

	@Override
	public void flush() {
		// do nothing
	}
	
	protected boolean isReady() throws IOException {
		return channel.isOpen();
	}

	boolean isClosed() {
		return isClosed;
	}

	void setClosed(boolean isClosed) {
		this.isClosed = isClosed;
	}

	public boolean isBufferOutput() {
		return bufferOutput;
	}

	public void setBufferOutput(boolean bufferOutput) {
		this.bufferOutput = bufferOutput;
	}
}
