package be.nabu.utils.io.containers.chars;

import java.io.IOException;

import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.CharBuffer;
import be.nabu.utils.io.api.WritableContainer;

/**
 * Performs a 1-1 mapping from bytes to chars where the exact byte value is translated to a char
 * This is not the same as code page 437 but instead conforms to http://www.unicode.org/charts/PDF/U0080.pdf
 * 
 * Note that this does not buffer the content, it instead uses peek() + skip() to ensure the target buffer always has the correct state
 */
public class WritableStraightByteToCharContainer implements WritableContainer<ByteBuffer> {


	private byte [] singleByte = new byte[1];
	private char [] singleChar = new char[1];
	private WritableContainer<CharBuffer> chars;
	
	public WritableStraightByteToCharContainer(WritableContainer<CharBuffer> chars) {
		this.chars = chars;
	}

	@Override
	public void close() throws IOException {
		chars.close();
	}

	@Override
	public long write(ByteBuffer target) throws IOException {
		long totalwritten = 0;
		while (target.remainingData() > 0) {
			target.peek(IOUtils.wrap(singleByte, false));
			singleChar[0] = (char) (singleByte[0] & 0xff);
			long written = chars.write(IOUtils.wrap(singleChar, true));
			if (written == -1) {
				return totalwritten == 0 ? -1 : totalwritten;
			}
			else if (written == 0) {
				break;
			}
			else {
				target.skip(1);
				totalwritten++;
			}
		}
		return totalwritten;
	}

	@Override
	public void flush() throws IOException {
		// do nothing
	}
}
