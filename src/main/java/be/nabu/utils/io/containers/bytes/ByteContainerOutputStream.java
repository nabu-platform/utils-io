package be.nabu.utils.io.containers.bytes;

import java.io.IOException;
import java.io.OutputStream;

import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.WritableContainer;

public class ByteContainerOutputStream extends OutputStream {

	private WritableContainer<ByteBuffer> container;
	private byte [] single = new byte [1];
	private boolean failIfFull;
	
	public ByteContainerOutputStream(WritableContainer<ByteBuffer> container, boolean failIfFull) {
		this.container = container;
		this.failIfFull = failIfFull;
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
		while (len > 0) {
			long written = container.write(IOUtils.wrap(b, off, len, true));
			if (written < 0) {
				throw new IOException("The target container is closed");
			}
			// there is no way to send back partial success
			// you "could" hang until it works but this might result in everlasting hangs
			else if (written == 0 && failIfFull) {
				throw new IOException("The target container is full");
			}
			len -= written;
		}
	}

	@Override
	public void write(byte[] b) throws IOException {
		write(b, 0, b.length);
	}
	
}
