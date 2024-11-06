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

import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.CharBuffer;
import be.nabu.utils.io.api.ReadableContainer;

public class ValidatedReadableCharContainer implements ReadableContainer<CharBuffer> {

	private ReadableContainer<CharBuffer> parent;
	private String characters;
	private boolean whitelist = true;
	
	/**
	 * Whitelist means only the passed in characters are allowed, blacklist means they will not be allowed
	 */
	public ValidatedReadableCharContainer(ReadableContainer<CharBuffer> parent, char [] characters, boolean whitelist) {
		this.parent = parent;
		this.characters = new String(characters);
		this.whitelist = whitelist;
	}
	
	@Override
	public void close() throws IOException {
		parent.close();
	}

	@Override
	public long read(CharBuffer target) throws IOException {
		long read = parent.read(target);
		CharBuffer copy = target.getFactory().newInstance(target.remainingData(), false);
		target.peek(copy);
		char [] chars = IOUtils.toChars(copy);
		// validate
		for (int i = 0; i < chars.length; i++) {
			int index = characters.indexOf(chars[i]);
			if ((whitelist && index == -1) || (!whitelist && index >= 0))
				throw new IOException("Illegal character found in data: " + chars[i] + " (" + (int) chars[i] + ")");
		}
		return read;
	}

}
