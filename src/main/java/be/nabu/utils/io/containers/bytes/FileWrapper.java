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
	public void flush() {
		// do nothing
	}

}
