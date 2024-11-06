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

public class PushbackContainerImpl<T extends Buffer<T>> extends BasePushbackContainer<T> {

	private ReadableContainer<T> container;
	private boolean closed;
	private boolean read;
	
	public PushbackContainerImpl(ReadableContainer<T> container) {
		this.container = container;
	}
	
	@Override
	public long read(T target) throws IOException {
		read = true;
		long totalRead = 0;
		if (getBuffer() != null && getBuffer().remainingData() > 0) {
			totalRead += getBuffer().read(target);
		}
		if (target.remainingSpace() > 0) {
			long read = container.read(target);
			if (read == -1) {
				closed = true;
			}
			else {
				totalRead += read;
			}
		}
		return totalRead == 0 && closed ? -1 : totalRead;
	}

	@Override
	public void close() throws IOException {
		closed = true;
		container.close();
	}

	public boolean isRead() {
		return read;
	}

	public void setRead(boolean read) {
		this.read = read;
	}
}
