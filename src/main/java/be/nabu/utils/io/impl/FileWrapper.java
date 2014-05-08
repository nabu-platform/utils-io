package be.nabu.utils.io.impl;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import be.nabu.utils.io.api.ByteContainer;
import be.nabu.utils.io.api.IORuntimeException;
import be.nabu.utils.io.api.LimitedReadableByteContainer;
import be.nabu.utils.io.api.MarkableByteContainer;
import be.nabu.utils.io.api.SkippableByteContainer;

public class FileWrapper implements ByteContainer, LimitedReadableByteContainer, SkippableByteContainer, MarkableByteContainer {

	private File file;
	private RandomAccessFile randomAccessFile;
	
	private long readPointer = 0;
	private long markPointer = 0;
	private long writePointer = 0;
	private boolean closed = false;
	
	public FileWrapper(File file) {
		this.file = file;
	}
	
	private RandomAccessFile getRandomAccessFile() {
		if (randomAccessFile == null) {
			try {
				if (!file.exists()) {
					if (!file.getParentFile().exists())
						file.mkdirs();
					file.createNewFile();
				}
				randomAccessFile = new RandomAccessFile(file, "rw");
			}
			catch (IOException e) {
				throw new IORuntimeException(e);
			}
		}
		return randomAccessFile;
	}
	
	@Override
	public int read(byte[] bytes) {
		return read(bytes, 0, bytes.length);
	}

	@Override
	public int read(byte[] bytes, int offset, int length) {
		try {
			if (readPointer >= getRandomAccessFile().length())
				return closed ? -1 : 0;
			else {
				getRandomAccessFile().seek(readPointer);
				int read = (int) Math.min(length, getRandomAccessFile().length() - readPointer);
				getRandomAccessFile().read(bytes, offset, read);
				readPointer += read;
				return read;
			}
		}
		catch (IOException e) {
			throw new IORuntimeException(e);
		}
	}

	@Override
	public void close() throws IOException {
		if (randomAccessFile != null)
			getRandomAccessFile().close();
		closed = true;
	}

	@Override
	public int write(byte[] bytes) {
		return write(bytes, 0, bytes.length);
	}

	@Override
	public int write(byte[] bytes, int offset, int length) {
		if (closed)
			throw new IllegalStateException("Can not write to a closed file");
		try {
			getRandomAccessFile().seek(writePointer);
			getRandomAccessFile().write(bytes, offset, length);
			writePointer += length;
			// if the size is beyond the pointer, truncate the rest
			if (getRandomAccessFile().length() > writePointer)
				getRandomAccessFile().getChannel().truncate(writePointer);
		}
		catch (IOException e) {
			throw new IORuntimeException(e);
		}
		return length;
	}

	@Override
	protected void finalize() throws Throwable {
		close();
	}

	@Override
	public long remainingData() {
		try {
			return randomAccessFile == null ? 0 : getRandomAccessFile().length() - readPointer;
		}
		catch (IOException e) {
			throw new IORuntimeException(e);
		}
	}

	@Override
	public void flush() {
		// do nothing
	}

	@Override
	public long skip(long amount) {
		amount = Math.min(amount, remainingData());
		readPointer += amount;
		return amount;
	}

	@Override
	public void reset() {
		readPointer = markPointer;
	}

	@Override
	public void mark() {
		markPointer = readPointer;
	}

	@Override
	public void unmark() {
		markPointer = 0;
	}
}
