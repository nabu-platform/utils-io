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

import be.nabu.utils.io.api.CharBuffer;
import be.nabu.utils.io.api.ReadableContainer;

public class ReaderWrapper implements ReadableContainer<CharBuffer> {

	private Reader input;
	private char [] buffer = new char[4096];
	
	public ReaderWrapper(Reader input) {
		this.input = input;
	}
	
	@Override
	public void close() throws IOException {
		input.close();
	}

	@Override
	public long read(CharBuffer target) throws IOException {
		long totalRead = 0;
		while (target.remainingSpace() > 0) {
			int read = input.read(buffer, 0, (int) Math.min(buffer.length, target.remainingSpace()));
			if (read == -1) {
				if (totalRead == 0)
					totalRead = -1;
				break;
			}
			target.write(buffer, 0, read);
			totalRead += read;
		}
		return totalRead;
	}

}
