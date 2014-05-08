package be.nabu.utils.io.impl;

import java.io.IOException;

import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.WritableCharContainer;

public class BufferedWritableCharContainer implements WritableCharContainer {

	private WritableCharContainer parent;
	private CharBufferWrapper buffer;
	private boolean closed = false;
	
	public BufferedWritableCharContainer(WritableCharContainer parent, int bufferSize) {
		this.parent = parent;
		this.buffer = new CharBufferWrapper(bufferSize);
	}
	
	@Override
	public void close() throws IOException {
		flush();
		parent.close();
	}

	@Override
	public int write(char[] chars) {
		return write(chars, 0, chars.length);
	}

	@Override
	public int write(char[] chars, int offset, int length) {
		int totalWritten = 0;
		while (length > 0) {
			int written = buffer.write(chars, offset, length);
			if (written == 0) {
				if (closed)
					return -1;
				long parentWrite = IOUtils.copy(buffer, parent);
				if (parentWrite == -1) {
					closed = true;
					break;
				}
				// no more space in parent atm
				else if (parentWrite == 0)
					break;
			}
			else {
				offset += written;
				length -= written;
				totalWritten += written;
			}
		}
		return totalWritten;
	}

	@Override
	public void flush() {
		IOUtils.copy(buffer, parent);
		parent.flush();
	}

}
