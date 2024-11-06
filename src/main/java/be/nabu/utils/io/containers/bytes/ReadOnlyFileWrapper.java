/*
* Copyright (C) 2014 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package be.nabu.utils.io.containers.bytes;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.DuplicatableContainer;
import be.nabu.utils.io.api.LimitedReadableContainer;
import be.nabu.utils.io.api.MarkableContainer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.api.SkippableContainer;

public class ReadOnlyFileWrapper implements ReadableContainer<ByteBuffer>, LimitedReadableContainer<ByteBuffer>, SkippableContainer<ByteBuffer>, MarkableContainer<ByteBuffer>, DuplicatableContainer<ByteBuffer, ReadOnlyFileWrapper> {

	private File file;
	private RandomAccessFile randomAccessFile;
	private long readPointer = 0;
	private long markPointer = 0;
	protected boolean closed = false;
	protected byte [] buffer = new byte[16384];
	private String mode;
	private boolean closeOnFullyRead = false;
	
	public ReadOnlyFileWrapper(File file) {
		this(file, "r");
	}
	
	protected ReadOnlyFileWrapper(File file, String mode) {
		this.file = file;
		this.mode = mode;
	}
	
	protected RandomAccessFile getRandomAccessFile() throws IOException {
		if (randomAccessFile == null) {
			if (!file.exists()) {
				if (!file.getParentFile().exists())
					file.mkdirs();
				file.createNewFile();
			}
			randomAccessFile = new RandomAccessFile(file, mode);
		}
		return randomAccessFile;
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

	@Override
	public long remainingData() {
		try {
			return !file.exists() ? 0 : getRandomAccessFile().length() - readPointer;
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public long read(ByteBuffer target) throws IOException {
		if (!file.exists() || readPointer >= getRandomAccessFile().length()) {
			if (closeOnFullyRead) {
				close();
			}
			return closed ? -1 : 0;
		}
		else {
			getRandomAccessFile().seek(readPointer);
			long totalRead = 0;
			while (target.remainingSpace() > 0) {
				int read = getRandomAccessFile().read(buffer, 0, (int) Math.min(target.remainingSpace(), buffer.length));
				if (read == -1) {
					if (totalRead == 0)
						totalRead = -1;
					break;
				}
				else if (read == 0)
					break;
				target.write(buffer, 0, read);
				readPointer += read;
				totalRead += read;
			}
			return totalRead;
		}
	}

	@Override
	public void close() throws IOException {
		if (randomAccessFile != null)
			getRandomAccessFile().close();
		closed = true;
	}

	@Override
	public ReadOnlyFileWrapper duplicate(boolean reset) {
		ReadOnlyFileWrapper wrapper = new ReadOnlyFileWrapper(file);
		if (!reset)
			wrapper.readPointer = readPointer;
		wrapper.closeOnFullyRead = true;
		return wrapper;
	}

	@Override
	protected void finalize() throws Throwable {
		close();
	}

	@Override
	public void remark() {
		unmark();
		mark();
	}

	public boolean isCloseOnFullyRead() {
		return closeOnFullyRead;
	}

	public void setCloseOnFullyRead(boolean closeOnFullyRead) {
		this.closeOnFullyRead = closeOnFullyRead;
	}
}
