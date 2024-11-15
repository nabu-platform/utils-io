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

public class BufferedReadableContainer<T extends Buffer<T>> implements ReadableContainer<T> {

	private ReadableContainer<T> parent;
	private T buffer;
	private boolean closed = false;

	// suppose you are buffering over a blocking source (e.g. inputstream)
	// if the buffer size is 10 bytes and the parent gives you 2,
	// you copy 2 to the source and again request 10 which would hang until the inputstream has more data
	// for this reason the buffer will stop pestering the parent if it has sent a suboptimal data amount before AND the buffer has already copied _something_ to the target
	boolean parentMightBeEmpty = false;
	
	public BufferedReadableContainer(ReadableContainer<T> parent, T buffer) {
		this.parent = parent;
		this.buffer = buffer;
	}
	
	@Override
	public void close() throws IOException {
		closed = true;
		parent.close();
	}

	@Override
	public long read(T target) throws IOException {
		long totalRead = 0;
		
		while (target.remainingSpace() > 0) {
			if (buffer.remainingData() > 0)
				totalRead += buffer.read(target);
			else if (parentMightBeEmpty && totalRead > 0)
				break;
			else {
				long read = parent.read(buffer);
				if (buffer.remainingSpace() > 0)
					parentMightBeEmpty = true;
				else
					parentMightBeEmpty = false;
				if (read == -1) {
					closed = true;
					break;
				}
				else if (read == 0)
					break;
			}
		}
		return totalRead == 0 && closed ? -1 : totalRead;
	}
}
