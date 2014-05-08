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
	private CyclicCharBuffer single = new CyclicCharBuffer(1);
	private CyclicCharBuffer read;
	private boolean stopped = false;
	private boolean isRegex = false;
	private boolean matched = false;
	private int bufferSize;
	
	public DelimitedCharContainerImpl(ReadableContainer<CharBuffer> parent, String delimiter) {
		this.parent = parent;
		this.delimiter = delimiter;
		this.buffer = new CyclicCharBuffer(delimiter.length());
		this.read = new CyclicCharBuffer(delimiter.length());
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
		this.read = new CyclicCharBuffer(bufferSize);
		this.bufferSize = bufferSize;
	}
	
	@Override
	public void close() throws IOException {
		parent.close();
	}

	@Override
	public long read(CharBuffer target) throws IOException {
		// if matched or stopped, return anything still in the buffer, otherwise -1
		if (matched || stopped)
			return buffer.remainingData() > 0 ? buffer.read(target) : -1;
			
		int amountRead = 0;
		long readFromParent = 0; 
		while (target.remainingSpace() > 0 && (readFromParent = parent.read(single)) == 1) {
			buffer.write(single);
			int amount = (int) buffer.peek(read);
			String stringContent = IOUtils.toString(read);
			// check for a regex match
			if (isRegex && stringContent.matches(delimiter)) {
				matched = true;
				// the regex may match only a part of the buffer
				// remove one character at a time from the resulting string and see when it no longer matches
				int index = 0;
				buffer.truncate();
				while(index < stringContent.length() && stringContent.substring(index + 1).matches(delimiter)) {
					buffer.write(new char[] { stringContent.charAt(index) });
					index++;
				}
				break;
			}
			// or a non-regex match where the size must always be respected
			else if (amount == bufferSize && !isRegex && delimiter.equals(stringContent)) {
				matched = true;
				// remove all data from the buffer, it is the delimiter
				buffer.truncate();
				break;
			}
			else if (amount == bufferSize) {
				// get a single char from the buffer and put it in the target
				buffer.read(single);
				target.write(single);
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
		return matched;
	}

}
