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

import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.Buffer;
import be.nabu.utils.io.api.ReadableContainer;

public class ReadableContainerChainer<T extends Buffer<T>> implements ReadableContainer<T> {

	private ReadableContainer<T> [] sources;
	private int active = 0;
	private boolean closeIfRead = true;
	private boolean allowEmptyReads = false;
	
	public ReadableContainerChainer(boolean closeIfRead, ReadableContainer<T>...sources) {
		this.closeIfRead = closeIfRead;
		this.sources = sources;
	}
	
	@Override
	public void close() throws IOException {
		IOUtils.close(sources);
	}

	@Override
	public long read(T target) throws IOException {
		long totalRead = 0;
		// as long as we have backing sources, get as much as possible
		while (target.remainingSpace() > 0 && active < sources.length) { 
			long read = sources[active].read(target);
			if (read < 0 || (read == 0 && !allowEmptyReads)) {
				if (closeIfRead)
					sources[active].close();
				active++;
			}
			// currently the backing source has no more but maybe later, check back soon!
			else if (read == 0) {
				break;
			}
			else
				totalRead += read;
		}
		// no more data
		if (totalRead == 0 && active >= sources.length) {
			totalRead = -1;
		}
		return totalRead;
	}

	public boolean isAllowEmptyReads() {
		return allowEmptyReads;
	}

	public void setAllowEmptyReads(boolean allowEmptyReads) {
		this.allowEmptyReads = allowEmptyReads;
	}
}
