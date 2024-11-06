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
import be.nabu.utils.io.api.DelimitedCharContainer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.buffers.chars.CyclicCharBuffer;

public class DelimitedCharContainerImpl implements ReadableContainer<CharBuffer>, DelimitedCharContainer {

	private CyclicCharBuffer buffer;
	private String delimiter;
	private ReadableContainer<CharBuffer> parent;
	private boolean stopped = false;
	private boolean isRegex = false;
	private int bufferSize;
	private String matchedDelimiter = null;
	// an escape sequence is tricky if it involves multiple characters, cause this would need to be accounted for in the bufferSize
	// we might add this at some point but we likely won't need it, until then we support a single escape character, usually \
	// we assume that an even amount of escape characters cancel each other out
	private char escapeCharacter = 0;
	
	// whether we are currently in escaped mode, this is toggled on by the escape character and toggled off by the character behind it
	private boolean escaped = false;
	
	public DelimitedCharContainerImpl(ReadableContainer<CharBuffer> parent, String delimiter) {
		this.parent = parent;
		this.delimiter = delimiter;
		this.buffer = new CyclicCharBuffer(delimiter.length());
		this.bufferSize = delimiter.length();
	}

	/**
	 * You can also set a regex as delimiter but then you need to specify a buffer size because a regex is rarely fixed-length
	 * The regex could match smaller strings then the full buffer, you can turn this off by forcing matchSize to true
	 */
	public DelimitedCharContainerImpl(ReadableContainer<CharBuffer> parent, String regex, int bufferSize) {
		this.parent = parent;
		this.delimiter = regex;
		this.isRegex = true;
		this.buffer = new CyclicCharBuffer(bufferSize);
		this.bufferSize = bufferSize;
	}
	
	@Override
	public void close() throws IOException {
		parent.close();
	}

	private char [] peek(CharBuffer buffer) throws IOException {
		char [] content = new char[(int) buffer.remainingData()];
		if (buffer.peek(IOUtils.wrap(content, false)) != content.length) {
			throw new IOException("Could not peek all content");
		}
		return content;
	}
	
	@Override
	public long read(CharBuffer target) throws IOException {
		// if matched or stopped, return anything still in the buffer, otherwise -1
		if (matchedDelimiter != null || stopped)
			return buffer.remainingData() > 0 ? buffer.read(target) : -1;
			
		int amountRead = 0;
		long readFromParent = 0;
		while (target.remainingSpace() > 0 && (readFromParent = parent.read(target.getFactory().limit(buffer, null, 1l))) == 1) {
			String stringContent = new String(peek(buffer));
			
			// when we are in escape mode, we want to stream the next character (even if it is the escape character itself) unchanged
			// if we are not in escape mode, we want to toggle the escape mode on if we encounter the escape character
			// in that case, we also want to send the escape character!
			if (escapeCharacter > 0 && (escaped || stringContent.charAt(stringContent.length() - 1) == escapeCharacter)) {
				// reverse the escaped
				escaped = !escaped;
				
				// get a single char from the buffer and put it in the target
				buffer.read(target.getFactory().limit(target, null, 1l));
				amountRead++;
				continue;
			}
			
			// check for a regex match
			if (isRegex && stringContent.matches(delimiter)) {
				// the regex may match only a part of the buffer
				// remove one character at a time from the resulting string and see when it no longer matches
				int index = 0;
				buffer.truncate();
				char [] single = new char[1];
				while(index < stringContent.length() && stringContent.substring(index + 1).matches(delimiter)) {
					single[0] = stringContent.charAt(index);
					buffer.write(single);
					index++;
				}
				matchedDelimiter = stringContent.substring(index);
				break;
			}
			// or a non-regex match where the size must always be respected
			else if (stringContent.length() == bufferSize && !isRegex && delimiter.equals(stringContent)) {
				// remove all data from the buffer, it is the delimiter
				matchedDelimiter = IOUtils.toString(buffer);
				break;
			}
			else if (stringContent.length() == bufferSize) {
				// get a single char from the buffer and put it in the target
				buffer.read(target.getFactory().limit(target, null, 1l));
				amountRead++;
			}
		}
		
		// stop if the parent is closed
		if (readFromParent == -1)
			stopped = true;

		// if the parent has no more data and we have no more data, just return -1
		// this happens when opening a delimiting container on an empty/closed parent container
		if (amountRead == 0 && buffer.remainingData() == 0 && stopped)
			return -1;
		else
			// if the amountRead is 0 but we still have data in the buffer, return it, otherwise we will return 0 which would stop a lot of readers
			return amountRead == 0 && buffer.remainingData() > 0 ? buffer.read(target) : amountRead;
	}

	@Override
	public boolean isDelimiterFound() {
		return matchedDelimiter != null;
	}

	@Override
	public String getMatchedDelimiter() {
		return matchedDelimiter;
	}

	public char getEscapeCharacter() {
		return escapeCharacter;
	}

	public void setEscapeCharacter(char escapeCharacter) {
		this.escapeCharacter = escapeCharacter;
	}

}
