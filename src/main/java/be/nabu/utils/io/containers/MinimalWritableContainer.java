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

package be.nabu.utils.io.containers;

import java.io.IOException;

import be.nabu.utils.io.api.Buffer;
import be.nabu.utils.io.api.WritableContainer;

public class MinimalWritableContainer<T extends Buffer<T>> implements WritableContainer<T> {
	
	private WritableContainer<T> parent;
	private long alreadyWritten, minimumAmountToWrite;
	
	public MinimalWritableContainer(WritableContainer<T> parent, long minimumAmountToWrite) {
		this.parent = parent;
		this.minimumAmountToWrite = minimumAmountToWrite;
	}
	
	@Override
	public void close() throws IOException {
		parent.close();
	}

	@Override
	public long write(T source) throws IOException {
		int totalWritten = 0;
		// if we still need to read something and we haven't done so already, do so
		while (source.remainingData() > 0) {
			long write = parent.write(source);
			if (write == -1)
				return totalWritten == 0 ? -1 : totalWritten;
			else if (write == 0 && alreadyWritten >= minimumAmountToWrite)
				break;
			totalWritten += write;
			alreadyWritten += write;
		}
		return totalWritten;
	}

	@Override
	public void flush() throws IOException {
		parent.flush();
	}
}
