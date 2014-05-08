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
