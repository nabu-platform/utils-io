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
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.api.WritableContainer;

public class ReadableContainerDuplicator<T extends Buffer<T>> implements ReadableContainer<T> {
	
	private ReadableContainer<T> parent;
	private WritableContainer<T> [] targets;
	private T buffer;
	private boolean closed = false;
	
	public ReadableContainerDuplicator(ReadableContainer<T> parent, WritableContainer<T>...targets) {
		this.parent = parent;
		this.targets = targets;
	}

	@Override
	public void close() throws IOException {
		closed = true;
		IOException exception = null;
		try {
			for (WritableContainer<T> target : targets) {
				try {
					target.close();
				}
				catch(IOException e) {
					exception = e;
				}
			}
		}
		finally {
			parent.close();
		}
		if (exception != null)
			throw exception;
	}

	@Override
	public long read(T targetBuffer) throws IOException {
		long totalRead = parent.read(targetBuffer);
		if (totalRead == -1) {
			return totalRead;
		}
		if (buffer != null)
			buffer.truncate();

		if (buffer == null || buffer.remainingSpace() < totalRead)
			buffer = targetBuffer.getFactory().newInstance(totalRead, false);
		
		for (WritableContainer<T> target : targets) {
			targetBuffer.peek(buffer);
			target.write(buffer);
			if (buffer.remainingData() > 0)
				throw new IOException("The target writable container did not have enough room to push the data to");
			buffer.truncate();
		}
		return totalRead == 0 && closed ? -1 : totalRead;
	}
}
