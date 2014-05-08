package be.nabu.utils.io.impl;

import java.io.IOException;

import be.nabu.utils.io.api.ReadableByteContainer;
import be.nabu.utils.io.api.ReadableCharContainer;

/**
 * Performs a 1-1 mapping from bytes to chars where the exact byte value is translated to a char
 * This is not the same as code page 437 but instead conforms to http://www.unicode.org/charts/PDF/U0080.pdf
 */
public class ReadableStraightByteToCharContainer implements ReadableCharContainer {

	private ReadableByteContainer bytes;

	private byte [] single = new byte[1];
	
	public ReadableStraightByteToCharContainer(ReadableByteContainer bytes) {
		this.bytes = bytes;
	}

	@Override
	public void close() throws IOException {
		bytes.close();
	}

	@Override
	public int read(char [] chars) {
		return read(chars, 0, chars.length);
	}

	@Override
	public int read(char[] chars, int offset, int length) {
		int totalRead = 0;
		while (length > 0) {
			int read = bytes.read(single);
			if (read == -1)
				return totalRead == 0 ? -1 : totalRead;
			else if (read == 0)
				break;
			chars[offset++] = (char) (single[0] & 0xff);
			length--;
			totalRead++;
		}
		return totalRead;
	}
}
