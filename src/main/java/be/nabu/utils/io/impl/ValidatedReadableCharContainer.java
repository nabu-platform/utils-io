package be.nabu.utils.io.impl;

import java.io.IOException;

import be.nabu.utils.io.api.IORuntimeException;
import be.nabu.utils.io.api.ReadableCharContainer;

public class ValidatedReadableCharContainer implements ReadableCharContainer {

	private ReadableCharContainer parent;
	private String characters;
	private boolean whitelist = true;
	
	/**
	 * Whitelist means only the passed in characters are allowed, blacklist means they will not be allowed
	 */
	public ValidatedReadableCharContainer(ReadableCharContainer parent, char [] characters, boolean whitelist) {
		this.parent = parent;
		this.characters = new String(characters);
		this.whitelist = whitelist;
	}
	
	@Override
	public void close() throws IOException {
		parent.close();
	}

	@Override
	public int read(char[] chars) {
		return read(chars, 0, chars.length);
	}

	@Override
	public int read(char[] chars, int offset, int length) {
		int read = parent.read(chars, offset, length);
		// validate
		for (int i = offset; i < read; i++) {
			int index = characters.indexOf(chars[i]);
			if ((whitelist && index == -1) || (!whitelist && index >= 0))
				throw new IORuntimeException("Illegal character found in data: " + chars[i] + " (" + (int) chars[i] + ")");
		}
		return read;
	}

}
