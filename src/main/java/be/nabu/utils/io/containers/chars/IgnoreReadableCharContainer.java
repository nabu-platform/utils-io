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
import java.util.Arrays;
import java.util.List;

import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.CharBuffer;
import be.nabu.utils.io.api.ReadableContainer;

public class IgnoreReadableCharContainer implements ReadableContainer<CharBuffer> {

	private ReadableContainer<CharBuffer> parent;
	private char [] single = new char[1];
	private List<Character> charactersToIgnore;
	
	public IgnoreReadableCharContainer(ReadableContainer<CharBuffer> parent, Character...charactersToIgnore) {
		this.parent = parent;
		this.charactersToIgnore = Arrays.asList(charactersToIgnore);
	}
	
	@Override
	public void close() throws IOException {
		parent.close();
	}

	@Override
	public long read(CharBuffer buffer) throws IOException {
		char [] tmp = new char[(int) buffer.remainingSpace()];
		long totalAmount = 0;
		// we want to guarantee reading at least _something_ if everything is ignored in a single read
		// this may block backends that stream only ignored characters but returning 0 might have the effect of stopping readers
		while (totalAmount == 0) {
			long read = parent.read(IOUtils.wrap(tmp, false));
			if (read <= 0) {
				return read;
			}
			else {
				for (int i = 0; i < read; i++) {
					if (!charactersToIgnore.contains(tmp[i])) {
						single[0] = tmp[i];
						buffer.write(single);
						totalAmount++;
					}
				}
			}
		}
		return totalAmount;
	}

}
