package be.nabu.utils.io.containers.chars;

import java.io.IOException;

import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.CharBuffer;
import be.nabu.utils.io.api.ReadableContainer;

public class HexReadableCharContainer implements ReadableContainer<CharBuffer> {

	private ReadableContainer<ByteBuffer> bytes;
	private ByteBuffer buffer = IOUtils.newByteBuffer();
	private byte[] singleByte = new byte[1];
	private boolean closed = false;
	
	public HexReadableCharContainer(ReadableContainer<ByteBuffer> bytes) {
		this.bytes = bytes;
	}
	
	@Override
	public void close() throws IOException {
		this.closed = true;
		bytes.close();
	}

	@Override
	public long read(CharBuffer target) throws IOException {
		long totalRead = 0;
		while (target.remainingSpace() > 0) {
			if (buffer.remainingData() == 0) {
				long read = bytes.read(buffer);
				if (read == 0)
					break;
				else if (read == -1) {
					closed = true;
					break;
				}
			}
			if (buffer.remainingData() > 0) {
				if (buffer.read(singleByte) != 1)
					throw new IOException("Could not read from temporary buffer");
				String formatted = String.format("%02x", singleByte[0] & 0xff);
				if (target.write(formatted.toCharArray()) != formatted.length())
					throw new IOException("Could not write to target buffer");
				totalRead += formatted.length();
			}
		}
		return totalRead == 0 && closed ? -1 : totalRead;
	}
	
}
