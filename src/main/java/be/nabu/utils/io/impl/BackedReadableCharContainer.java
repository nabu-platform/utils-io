package be.nabu.utils.io.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;

import be.nabu.utils.io.api.EncodingRuntimeException;
import be.nabu.utils.io.api.IORuntimeException;
import be.nabu.utils.io.api.ReadableByteContainer;
import be.nabu.utils.io.api.ReadableCharContainer;

public class BackedReadableCharContainer implements ReadableCharContainer {

	private ReadableByteContainer byteContainer;
	private CharsetDecoder decoder;
	private CharBuffer charBuffer;
	private ByteBuffer byteBuffer;
	private boolean closed = false;
	private byte [] newBytes = new byte[2056];
	private int processed = -1;
	
	public BackedReadableCharContainer(ReadableByteContainer byteContainer, Charset charset) {
		this.byteContainer = byteContainer;
		this.decoder = charset.newDecoder();
		this.charBuffer = CharBuffer.allocate(1024);
		this.charBuffer.limit(0);
	}
	
	@Override
	public void close() throws IOException {
		// this error is at first glance valid
		// but it occurs almost exclusively when trying to close an already faulty data stream
		// in this case it often suppresses the actual exception so for now it will be disabled
//		if (byteBuffer != null && byteBuffer.remaining() > 0)
//			throw new IOException("Can not close character converter, it still has unprocessed content");
		byteContainer.close();
	}

	@Override
	public int read(char [] chars) {
		return read(chars, 0, chars.length);
	}

	@Override
	public int read(char [] chars, int offset, int length) {
		int readChars = 0;
		while (length > 0) {
			if (!charBuffer.hasRemaining()) {
				// no data left in the charBuffer and none in the byteContainer
				if (closed)
					return -1;
				if (byteBuffer == null || !byteBuffer.hasRemaining() || processed == 0) {
					int readBytes = byteContainer.read(newBytes);
					// byte container is closed
					if (readBytes == -1) {
						if (byteBuffer != null && byteBuffer.remaining() > 0)
							throw new IORuntimeException("The data stream is not complete, there are " + byteBuffer.remaining() + " remaining bytes");
						closed = true;
						if (readChars == 0)
							return -1;
						else
							return readChars;
					}
					// no data available, return what we have
					else if (readBytes == 0)
						return readChars;
					else {
						if (byteBuffer == null || !byteBuffer.hasRemaining())
							byteBuffer = ByteBuffer.allocate(readBytes);
						else {
							ByteBuffer newByteBuffer = ByteBuffer.allocate(readBytes + byteBuffer.remaining());
							if (byteBuffer.remaining() > 0) {
								byte [] remainingData = new byte[byteBuffer.remaining()];
								byteBuffer.get(remainingData);
								newByteBuffer.put(remainingData);
							}
							byteBuffer = newByteBuffer;
						}
						byteBuffer.put(newBytes, 0, readBytes);
						byteBuffer.flip();
					}
				}
				charBuffer.clear();
				// decode
				processed = byteBuffer.remaining();
				CoderResult result = decoder.decode(byteBuffer, charBuffer, closed);
				processed -= byteBuffer.remaining();
				if (result.isError()) {
					try {
						result.throwException();
					}
					catch (CharacterCodingException e) {
						throw new EncodingRuntimeException(e);
					}
				}
				charBuffer.flip();
			}
			int sizeToRead = Math.min(length, charBuffer.remaining());
			charBuffer.get(chars, offset, sizeToRead);
			readChars += sizeToRead;
			offset += sizeToRead;
			length -= sizeToRead;
		}
		return readChars;
	}

}
