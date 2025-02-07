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
import java.util.Date;

import be.nabu.utils.io.api.Buffer;
import be.nabu.utils.io.api.ReadableContainer;

/**
 * Allows you to specify a minimum amount that has to be read
 * The container will block until then!
 */
public class MinimalReadableContainer<T extends Buffer<T>> implements ReadableContainer<T> {

	private ReadableContainer<T> parent;
	private long alreadyRead, minimumAmountToRead, timeout;
	
	public MinimalReadableContainer(ReadableContainer<T> parent, long minimumAmountToRead) {
		this(parent, minimumAmountToRead, 0);
	}
	
	public MinimalReadableContainer(ReadableContainer<T> parent, long minimumAmountToRead, long timeout) {
		this.parent = parent;
		this.minimumAmountToRead = minimumAmountToRead;
		this.timeout = timeout;
	}
	
	@Override
	public void close() throws IOException {
		parent.close();
	}

	@Override
	public long read(T target) throws IOException {
		int totalRead = 0;
		Date started = timeout == 0 ? null : new Date();
		// if we still need to read something and we haven't done so already, do so
		while (target.remainingSpace() > 0) {
			long read = parent.read(target);
			if (read == -1) {
				return totalRead == 0 ? -1 : totalRead;
			}
			else if (read == 0 && alreadyRead >= minimumAmountToRead) {
				break;
			}
			// the minimum amount is not yet reached
			// check that the read did not time out (avoid long read attacks)
			else if (alreadyRead < minimumAmountToRead && started != null && new Date().getTime() - started.getTime() > timeout) {
				throw new IOException("The read timed out");
			}
			totalRead += read;
			alreadyRead += read;
		}
		return totalRead;
	}
}
