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

package be.nabu.utils.io.containers.chars;

import java.io.IOException;
import java.io.Reader;

import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.CharBuffer;
import be.nabu.utils.io.api.ReadableContainer;

public class CharContainerReader extends Reader {

	private ReadableContainer<CharBuffer> container;
	
	private boolean closed = false;
	
	public CharContainerReader(ReadableContainer<CharBuffer> container) {
		this.container = container;
	}
	
	@Override
	public void close() throws IOException {
		closed = true;
		container.close();
	}

	@Override
	public int read(char[] characters, int offset, int length) throws IOException {
		if (closed)
			return -1;
			
		int read = 0;
		while (length > 0 && read == 0)
			read = (int) container.read(IOUtils.wrap(characters, offset, length, false));
		
		return read;
	}

}
