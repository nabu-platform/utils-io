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
import java.io.Writer;

import be.nabu.utils.io.api.CharBuffer;
import be.nabu.utils.io.api.WritableContainer;

public class WriterWrapper implements WritableContainer<CharBuffer> {

	private Writer output;
	private char [] buffer = new char[4096];
	
	public WriterWrapper(Writer output) {
		this.output = output;
	}
	
	@Override
	public void close() throws IOException {
		output.close();
	}

	@Override
	public long write(CharBuffer source) throws IOException {
		long totalWritten = 0;
		while (source.remainingData() > 0) {
			int read = source.read(buffer);
			output.write(buffer, 0, read);
			totalWritten += read;
		}
		return totalWritten;
	}

	@Override
	public void flush() throws IOException {
		output.flush();
	}

}
