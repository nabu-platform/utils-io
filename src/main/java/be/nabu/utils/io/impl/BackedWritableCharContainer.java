package be.nabu.utils.io.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

import be.nabu.utils.io.api.EncodingRuntimeException;
import be.nabu.utils.io.api.IORuntimeException;
import be.nabu.utils.io.api.WritableByteContainer;
import be.nabu.utils.io.api.WritableCharContainer;

public class BackedWritableCharContainer implements WritableCharContainer {

	private WritableByteContainer byteContainer;
	private CharsetEncoder encoder;
	
	private CharBuffer charBuffer;
	private ByteBuffer byteBuffer;
	private boolean closed = false;
	
	/**
	 * When you flush you read from the bytebuffer
	 */
	private boolean wasReading = false;
	
	public BackedWritableCharContainer(WritableByteContainer container, Charset charset) {
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
	public int write(char[] chars) {
		return write(chars, 0, chars.length);
	}

	@Override
	public int write(char[] chars, int offset, int length) {
		charBuffer = CharBuffer.wrap(chars, offset, length);
		if (wasReading) {
			wasReading = false;
			byteBuffer.compact();
		}
		while (charBuffer.hasRemaining()) {
			CoderResult result = encoder.encode(charBuffer, byteBuffer, closed);
			if (result.isError()) {
				try {
					result.throwException();
				}
				catch (CharacterCodingException e) {
					throw new EncodingRuntimeException(e);
				}
			}
			byteBuffer.flip();
			int actualWrite = byteContainer.write(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit() - byteBuffer.position());
			if (actualWrite == -1)
				throw new IORuntimeException("The target writable is closed");
			byteBuffer.position(byteBuffer.position() + actualWrite);
			byteBuffer.compact();
			if (actualWrite == 0)
				break;
		}
		return length - charBuffer.remaining();
	}

	@Override
	public void flush() {
		if (!wasReading) {
			wasReading = true;
			byteBuffer.flip();
		}
		// the write cycle always ends on a compact
		if (byteBuffer.remaining() > 0) {
			int remaining = byteBuffer.remaining();
			int flushed = byteContainer.write(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit() - byteBuffer.position());
			byteBuffer.position(byteBuffer.position() + flushed);
			byteContainer.flush();
			if (flushed != remaining)
				throw new IORuntimeException("Could only flush " + flushed + "/" + remaining + " bytes to the byte container");
		}
		else
			byteContainer.flush();
	}
}