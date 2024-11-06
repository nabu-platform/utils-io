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

public class TrailingContainer<T extends Buffer<T>> implements ReadableContainer<T> {

	private ReadableContainer<T> parent;
	private T buffer;
	private long trailSize;
	
	public TrailingContainer(ReadableContainer<T> parent, long trailSize) {
		this.parent = parent;
		this.trailSize = trailSize;
	}

	@Override
	public void close() throws IOException {
		parent.close();
	}

	@Override
	public long read(T target) throws IOException {
		long read = parent.read(target);
		
		if (buffer == null)
			buffer = target.getFactory().newInstance(trailSize, true);
		
		long amountToStore = Math.min(read, trailSize);
		if (amountToStore > 0) {
			long amountToSkip = amountToStore - buffer.remainingSpace();
			
			if (amountToSkip > 0)
				buffer.skip(amountToSkip);
			
			T copy = target.getFactory().newInstance(target.remainingData(), false);
			target.peek(copy);
			copy.skip(read - amountToStore);
			buffer.write(copy);
		}

		return read;
	}
	
	public ReadableContainer<T> getTrailing() {
		return buffer;
	}
}
