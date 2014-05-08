package be.nabu.utils.io.impl;

import java.io.IOException;

import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.WritableByteContainer;

public class BufferedWritableByteContainer implements WritableByteContainer {

	private WritableByteContainer parent;
	private ByteBufferWrapper buffer;
	private boolean closed = false;
	
	public BufferedWritableByteContainer(WritableByteContainer parent, int bufferSize) {
		this.parent = parent;
		this.buffer = new ByteBufferWrapper(bufferSize);
	}
	
	@Override
	public void close() throws IOException {
		flush();
		parent.close();
	}

	@Override
	public int write(byte[] bytes) {
		return write(bytes, 0, bytes.length);
	}

	@Override
	public int write(byte[] bytes, int offset, int length) {
		int totalWritten = 0;
		while (length > 0) {
			int written = buffer.write(bytes, offset, length);
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
