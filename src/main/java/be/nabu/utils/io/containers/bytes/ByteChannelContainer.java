package be.nabu.utils.io.containers.bytes;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.Container;

public class ByteChannelContainer<T extends ByteChannel> implements Container<be.nabu.utils.io.api.ByteBuffer> {
	
	private T channel;
	private byte [] bytes = new byte[4096];
	private boolean isClosed;

	public ByteChannelContainer(T channel) {
		this.channel = channel;
	}

	public T getChannel() {
		return channel;
	}

	@Override
	public long read(be.nabu.utils.io.api.ByteBuffer target) throws IOException {
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
	public long write(be.nabu.utils.io.api.ByteBuffer source) throws IOException {
		if (isClosed()) {
			return -1;
		}
		else if (!isReady()) {
			return 0;
		}
		long totalWritten = 0;
		while (source.remainingData() > 0) {
			int read = (int) source.peek(IOUtils.wrap(bytes, false));
			int written = channel.write(ByteBuffer.wrap(bytes, 0, read));
			// skip the data that was successfully written
			source.skip(written);
			totalWritten += written;
			if (written != read) {
				break;
			}
		}
		return totalWritten == 0 && isClosed() ? -1 : totalWritten;
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
}
