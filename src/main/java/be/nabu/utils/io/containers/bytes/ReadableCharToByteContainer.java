package be.nabu.utils.io.containers.bytes;

import java.io.IOException;
import java.nio.charset.Charset;

import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.CharBuffer;
import be.nabu.utils.io.api.Container;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.api.WritableContainer;
import be.nabu.utils.io.containers.EOFReadableContainer;

public class ReadableCharToByteContainer implements ReadableContainer<ByteBuffer> {

	private EOFReadableContainer<CharBuffer> parent;
	private Container<ByteBuffer> container = IOUtils.newByteBuffer(4096, true);
	private WritableContainer<CharBuffer> output;
	private boolean parentClosed = false;
	
	public ReadableCharToByteContainer(ReadableContainer<CharBuffer> parent, Charset charset) {
		this.parent = new EOFReadableContainer<CharBuffer>(parent);
		output = IOUtils.wrapWritable(container, charset);
	}
	
	@Override
	public void close() throws IOException {
		parent.close();
	}

	@Override
	public long read(ByteBuffer target) throws IOException {
		int totalRead = 0;
		while (target.remainingSpace() > 0) {
			long read = container.read(target);
			if (read == 0) {
				if (parentClosed)
					return -1;
				else if (IOUtils.copyChars(parent, output) == 0 && parent.isEOF()) {
					parentClosed = true;
					break;
				}
				else
					read = container.read(target);
			}
			if (read == 0)
				break;
			else
				totalRead += read;
		}
		return totalRead;
	}

}
