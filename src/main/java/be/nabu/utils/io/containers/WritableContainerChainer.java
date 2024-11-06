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
import be.nabu.utils.io.api.LimitedWritableContainer;
import be.nabu.utils.io.api.WritableContainer;

public class WritableContainerChainer<T extends Buffer<T>> implements WritableContainer<T> {

	private LimitedWritableContainer<T> [] targets;
	private int current = 0;
	private boolean closed = false;
	
	public WritableContainerChainer(LimitedWritableContainer<T>...targets) {
		this.targets = targets;
	}
	
	@Override
	public void close() throws IOException {
		closed = true;
		IOException exception = null;
		for (int i = current; i < targets.length; i++) {
			try {
				targets[i].close();
			}
			catch (IOException e) {
				exception = e;
			}
		}
		if (exception != null)
			throw exception;
	}

	@Override
	public long write(T buffer) throws IOException {
		long totalWritten = 0;
		while (buffer.remainingData() > 0) {
			if (current >= targets.length)
				return totalWritten == 0 && closed ? -1 : totalWritten;
			
			if (targets[current].remainingSpace() == 0) {
				targets[current].close();
				current++;
			}
			else
				totalWritten += targets[current].write(buffer);
		}
		return totalWritten;
	}

	@Override
	public void flush() throws IOException {
		if (current < targets.length)
			targets[current].flush();
	}

}
