package be.nabu.utils.io.containers.chars;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;

import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.CharBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.buffers.bytes.StaticByteBuffer;
import be.nabu.utils.io.buffers.chars.NioCharBufferWrapper;

public class BackedReadableCharContainer implements ReadableContainer<CharBuffer> {

	private ReadableContainer<ByteBuffer> byteContainer;
	private CharsetDecoder decoder;
	private java.nio.CharBuffer charBuffer;
	private java.nio.ByteBuffer byteBuffer;
	private boolean closed = false;
	private byte [] newBytes = new byte[2056];
	private int processed = -1;
	
	public BackedReadableCharContainer(ReadableContainer<ByteBuffer> byteContainer, Charset charset) {
		this.byteContainer = byteContainer;
		this.decoder = charset.newDecoder();
		this.charBuffer = java.nio.CharBuffer.allocate(1024);
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
	public long read(CharBuffer chars) throws IOException {
		int readChars = 0;
		while (chars.remainingSpace() > 0) {
			if (!charBuffer.hasRemaining()) {
				// no data left in the charBuffer and none in the byteContainer
				if (closed)
					return -1;
				if (byteBuffer == null || !byteBuffer.hasRemaining() || processed == 0) {
					int readBytes = (int) byteContainer.read(new StaticByteBuffer(newBytes, false));
					// byte container is closed
					if (readBytes == -1) {
						if (byteBuffer != null && byteBuffer.remaining() > 0)
							throw new IOException("The data stream is not complete, there are " + byteBuffer.remaining() + " remaining bytes");
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
							byteBuffer = java.nio.ByteBuffer.allocate(readBytes);
						else {
							java.nio.ByteBuffer newByteBuffer = java.nio.ByteBuffer.allocate(readBytes + byteBuffer.remaining());
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
				if (result.isError())
					result.throwException();
				charBuffer.flip();
			}
			int sizeToRead = (int) Math.min(chars.remainingSpace(), charBuffer.remaining());
			chars.write(new NioCharBufferWrapper(charBuffer, true));
//			charBuffer.get(chars, offset, sizeToRead);
			readChars += sizeToRead;
		}
		return readChars;
	}

}
