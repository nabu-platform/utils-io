package be.nabu.utils.io.containers.bytes;

import java.io.File;
import java.io.IOException;

import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.Container;

public class FileWrapper extends ReadOnlyFileWrapper implements Container<ByteBuffer> {

	private long writePointer = 0;

	public FileWrapper(File file) {
		super(file, "rw");
	}

	@Override
	public long write(ByteBuffer source) throws IOException {
		if (closed)
			throw new IllegalStateException("Can not write to a closed file");
		getRandomAccessFile().seek(writePointer);
		long totalWritten = 0;
		while (source.remainingData() > 0) {
			int read = source.read(buffer); 
			getRandomAccessFile().write(buffer, 0, read);
			writePointer += read;
			// if the size is beyond the pointer, truncate the rest
			if (getRandomAccessFile().length() > writePointer)
				getRandomAccessFile().getChannel().truncate(writePointer);
			totalWritten += read;
		}
		return totalWritten;
	}

	@Override
	protected void finalize() throws Throwable {
		close();
	}

	@Override
	public void flush() {
		// do nothing
	}

}
