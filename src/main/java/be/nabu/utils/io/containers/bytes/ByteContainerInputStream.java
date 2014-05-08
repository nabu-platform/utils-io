package be.nabu.utils.io.containers.bytes;

import java.io.IOException;
import java.io.InputStream;

import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;

public class ByteContainerInputStream extends InputStream {
	
	private ReadableContainer<ByteBuffer> container;
	private boolean closed = false;
	private byte [] single = new byte[1];
	
	public ByteContainerInputStream(ReadableContainer<ByteBuffer> container) {
		this.container = container;
	}
	
	@Override
	public void close() throws IOException {
		closed = true;
		container.close();
	}

	@Override
	public int read(byte [] bytes) throws IOException {
		return read(bytes, 0, bytes.length);
	}
	
	@Override
	public int read() throws IOException {
		int read = read(single);
		if (read == -1)
			return read;
		else
			return single[0] & 0xff;
	}
	
	@Override
	public int read(byte[] bytes, int offset, int length) throws IOException {
		if (closed)
			return -1;
		
		int read = 0;
		// block until data is present
		while (read == 0)
			read = (int) container.read(IOUtils.wrap(bytes, offset, length, false));
		return read;
	}

}
