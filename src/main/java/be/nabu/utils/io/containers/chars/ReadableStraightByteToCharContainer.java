package be.nabu.utils.io.containers.chars;

import java.io.IOException;

import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.CharBuffer;
import be.nabu.utils.io.api.ReadableContainer;

/**
 * Performs a 1-1 mapping from bytes to chars where the exact byte value is translated to a char
 * This is not the same as code page 437 but instead conforms to http://www.unicode.org/charts/PDF/U0080.pdf
 */
public class ReadableStraightByteToCharContainer implements ReadableContainer<CharBuffer> {

	private ReadableContainer<ByteBuffer> bytes;

	private byte [] singleByte = new byte[1];
	private char [] singleChar = new char[1];
	
	public ReadableStraightByteToCharContainer(ReadableContainer<ByteBuffer> bytes) {
		this.bytes = bytes;
	}

	@Override
	public void close() throws IOException {
		bytes.close();
	}

	@Override
	public long read(CharBuffer target) throws IOException {
		long totalRead = 0;
		while (target.remainingSpace() > 0) {
			long read = bytes.read(IOUtils.wrap(singleByte, false));
			if (read == -1)
				return totalRead == 0 ? -1 : totalRead;
			else if (read == 0)
				break;
			singleChar[0] = (char) (singleByte[0] & 0xff);
			totalRead += target.write(singleChar);
		}
		return totalRead;
	}
}
