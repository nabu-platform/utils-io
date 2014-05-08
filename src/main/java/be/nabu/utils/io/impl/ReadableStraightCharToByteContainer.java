package be.nabu.utils.io.impl;

import java.io.IOException;

import be.nabu.utils.io.api.ReadableByteContainer;
import be.nabu.utils.io.api.ReadableCharContainer;

/**
 * Performs a 1-1 mapping from chars to bytes where the exact byte value is translated from a char
 * This is not the same as code page 437 but instead conforms to http://www.unicode.org/charts/PDF/U0080.pdf
 */
public class ReadableStraightCharToByteContainer implements ReadableByteContainer {

	private ReadableCharContainer chars;

	private char [] single = new char[1];
	
	public ReadableStraightCharToByteContainer(ReadableCharContainer bytes) {
		this.chars = bytes;
	}

	@Override
	public void close() throws IOException {
		chars.close();
	}

	@Override
	public int read(byte [] bytes) {
		return read(bytes, 0, bytes.length);
	}

	@Override
	public int read(byte[] bytes, int offset, int length) {
		int totalRead = 0;
		while (length > 0) {
			int read = chars.read(single);
			if (read == -1)
				return totalRead == 0 ? -1 : totalRead;
			else if (read == 0)
				break;
			bytes[offset++] = (byte) (single[0] & 0xff);
			length--;
			totalRead++;
		}
		return totalRead;
	}
}
