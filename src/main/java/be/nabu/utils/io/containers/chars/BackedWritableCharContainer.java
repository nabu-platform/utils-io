package be.nabu.utils.io.containers.chars;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.WritableContainer;
import be.nabu.utils.io.buffers.bytes.StaticByteBuffer;

public class BackedWritableCharContainer implements WritableContainer<be.nabu.utils.io.api.CharBuffer> {

	private WritableContainer<be.nabu.utils.io.api.ByteBuffer> byteContainer;
	private CharsetEncoder encoder;
	
	private CharBuffer charBuffer;
	private ByteBuffer byteBuffer;
	private boolean closed = false;
	
	/**
	 * When you flush you read from the bytebuffer
	 */
	private boolean wasReading = false;
	
	public BackedWritableCharContainer(WritableContainer<be.nabu.utils.io.api.ByteBuffer> container, Charset charset) {
		this.byteContainer = container;
		this.encoder = charset.newEncoder();
		byteBuffer = ByteBuffer.allocate(2048);
		charBuffer = CharBuffer.allocate(1024);
	}
	
	@Override
	public void close() throws IOException {
		if (!closed) {
			flush();
			byteContainer.close();
			closed = true;
		}
	}

	@Override
	public long write(be.nabu.utils.io.api.CharBuffer source) throws IOException {
		long length = source.remainingData();
		// peek so we don't lose data if we get all chars but can't push all to backend
		charBuffer = CharBuffer.wrap(IOUtils.peekChars(source));
		if (wasReading) {
			wasReading = false;
			byteBuffer.compact();
		}
		while (charBuffer.hasRemaining()) {
			CoderResult result = encoder.encode(charBuffer, byteBuffer, closed);
			if (result.isError())
				result.throwException();
			byteBuffer.flip();
			int actualWrite = (int) byteContainer.write(new StaticByteBuffer(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit() - byteBuffer.position(), true));
			if (actualWrite == -1)
				throw new IOException("The target writable is closed");
			byteBuffer.position(byteBuffer.position() + actualWrite);
			byteBuffer.compact();
			if (actualWrite == 0)
				break;
		}
		long amountWritten = length - charBuffer.remaining();
		source.skip(amountWritten);
		return amountWritten;
	}

	@Override
	public void flush() throws IOException {
		if (!wasReading) {
			wasReading = true;
			byteBuffer.flip();
		}
		// the write cycle always ends on a compact
		if (byteBuffer.remaining() > 0) {
			int remaining = byteBuffer.remaining();
			int flushed = (int) byteContainer.write(new StaticByteBuffer(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit() - byteBuffer.position(), true));
			byteBuffer.position(byteBuffer.position() + flushed);
			byteContainer.flush();
			if (flushed != remaining)
				throw new IOException("Could only flush " + flushed + "/" + remaining + " bytes to the byte container " + byteContainer.getClass().getName());
		}
		else
			byteContainer.flush();
	}
}