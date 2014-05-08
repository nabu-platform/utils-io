package be.nabu.utils.io.impl;

import java.io.IOException;

import be.nabu.utils.io.api.DelimitedCharContainer;
import be.nabu.utils.io.api.ReadableCharContainer;

public class DelimitedCharContainerImpl implements DelimitedCharContainer {

	private CyclicCharContainer buffer;
	private String delimiter;
	private ReadableCharContainer parent;
	private char [] single = new char[1];
	private char [] read;
	private boolean stopped = false;
	private boolean isRegex = false;
	private boolean matched = false;
	
	public DelimitedCharContainerImpl(ReadableCharContainer parent, String delimiter) {
		this.parent = parent;
		this.delimiter = delimiter;
		this.buffer = new CyclicCharContainer(delimiter.length());
		this.read = new char[delimiter.length()];
	}

	/**
	 * You can also set a regex as delimiter but then you need to specify a buffer size because a regex is rarely fixed-length
	 * The regex could match smaller strings then the full buffer, you can turn this off by forcing matchSize to true
	 */
	public DelimitedCharContainerImpl(ReadableCharContainer parent, String regex, int bufferSize) {
		this.parent = parent;
		this.delimiter = regex;
		this.isRegex = true;
		this.buffer = new CyclicCharContainer(bufferSize);
		this.read = new char[bufferSize];
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
		// if matched or stopped, return anything still in the buffer, otherwise -1
		if (matched || stopped)
			return buffer.remainingData() > 0 ? buffer.read(chars, offset, length) : -1;
			
		int amountRead = 0;
		int readFromParent = 0; 
		while (length > 0 && (readFromParent = parent.read(single)) == 1) {
			buffer.write(single);
			int amount = buffer.peak(read);
			String stringContent = new String(read, 0, amount);
			// check for a regex match
			if (isRegex && stringContent.matches(delimiter)) {
				matched = true;
				// the regex may match only a part of the buffer
				// remove one character at a time from the resulting string and see when it no longer matches
				int index = 0;
				buffer.truncate();
				while(index < stringContent.length() && stringContent.substring(index + 1).matches(delimiter)) {
					single[0] = stringContent.charAt(index);
					buffer.write(single);
					index++;
				}
				break;
			}
			// or a non-regex match where the size must always be respected
			else if (amount == read.length && !isRegex && delimiter.equals(stringContent)) {
				matched = true;
				// remove all data from the buffer, it is the delimiter
				buffer.truncate();
				break;
			}
			else if (amount == read.length) {
				buffer.read(chars, offset++, 1);
				amountRead++;
				length--;
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
			return amountRead == 0 && buffer.remainingData() > 0 ? buffer.read(chars, offset, length) : amountRead;
	}

	@Override
	public boolean isDelimiterFound() {
		return matched;
	}

}
