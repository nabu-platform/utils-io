package be.nabu.utils.io.containers.bytes;

import java.io.IOException;
import java.io.OutputStream;

import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.WritableContainer;

public class ByteContainerOutputStream extends OutputStream {

	private WritableContainer<ByteBuffer> container;
	private byte [] single = new byte [1];
	
	public ByteContainerOutputStream(WritableContainer<ByteBuffer> container) {
		this.container = container;
	}
	
	@Override
	public void write(int arg0) throws IOException {
		single[0] = (byte) arg0;
		write(single);
	}

	@Override
	public void close() throws IOException {
		container.close();
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		while (len > 0)
			len -= container.write(IOUtils.wrap(b, off, len, true));
	}

	@Override
	public void write(byte[] b) throws IOException {
		write(b, 0, b.length);
	}
	
}
