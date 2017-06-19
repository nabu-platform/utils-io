package be.nabu.utils.io.containers.chars;

import java.io.IOException;

import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.CharBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.buffers.bytes.ByteBufferFactory;

/**
 * Performs a 1-1 mapping from bytes to chars where the exact byte value is translated to a char
 * This is not the same as code page 437 but instead conforms to http://www.unicode.org/charts/PDF/U0080.pdf
 */
public class ReadableStraightByteToCharContainer implements ReadableContainer<CharBuffer> {

	private ReadableContainer<ByteBuffer> bytes;
	private boolean closed;
	
	private byte [] byteBuffer = new byte[8096];
	private char [] charBuffer = new char[8096];
	
	public ReadableStraightByteToCharContainer(ReadableContainer<ByteBuffer> bytes) {
		this.bytes = bytes;
	}

	@Override
	public void close() throws IOException {
		bytes.close();
	}

	@Override
	public long read(CharBuffer target) throws IOException {
		long read = bytes.read(ByteBufferFactory.getInstance().limit(IOUtils.wrap(byteBuffer, false), null, Math.min(byteBuffer.length, target.remainingSpace())));
		if (read < 0) {
			closed = true;
		}
		else {
			for (int i = 0; i < read; i++) {
				charBuffer[i] = (char) (byteBuffer[i] & 0xff);
			}
			target.write(charBuffer, 0, (int) read);
		}
		return read <= 0 && closed ? -1 : read;
	}
}
