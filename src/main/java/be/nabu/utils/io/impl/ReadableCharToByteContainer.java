package be.nabu.utils.io.impl;

import java.io.IOException;
import java.nio.charset.Charset;

import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteContainer;
import be.nabu.utils.io.api.ReadableByteContainer;
import be.nabu.utils.io.api.ReadableCharContainer;
import be.nabu.utils.io.api.WritableByteContainer;
import be.nabu.utils.io.api.WritableCharContainer;

public class ReadableCharToByteContainer implements ReadableByteContainer {

	private EOFReadableCharContainer parent;
	private ByteContainer container = IOUtils.newByteContainer();
	private WritableCharContainer output;
	private boolean parentClosed = false;
	
	public ReadableCharToByteContainer(ReadableCharContainer parent, Charset charset) {
		this.parent = new EOFReadableCharContainer(parent);
		output = IOUtils.wrap((WritableByteContainer) container, charset);
	}
	
	@Override
	public void close() throws IOException {
		parent.close();
	}

	@Override
	public int read(byte[] bytes) {
		return read(bytes, 0, bytes.length);
	}

	@Override
	public int read(byte[] bytes, int offset, int length) {
		int totalRead = 0;
		while (length > 0) {
			int read = container.read(bytes, offset, length);
			if (read == 0) {
				if (parentClosed)
					return -1;
				else if (IOUtils.copy(IOUtils.limitReadable(parent, length), output) == 0 && parent.isEOF()) {
					parentClosed = true;
					break;
				}
				else
					read = container.read(bytes, offset, length);
			}
			if (read == 0)
				break;
			else {
				length -= read;
				offset += read;
				totalRead += read;
			}
		}
		return totalRead;
	}

}
