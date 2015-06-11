package be.nabu.utils.io.containers.chars;

import java.io.IOException;

import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.CharBuffer;
import be.nabu.utils.io.api.DelimitedCharContainer;
import be.nabu.utils.io.api.PushbackContainer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.api.ResettableContainer;
import be.nabu.utils.io.buffers.chars.CyclicCharBuffer;
import be.nabu.utils.io.containers.BasePushbackContainer;

/**
 * This is a delimited container that should be backed by a marked container 
 */
public class BackedDelimitedCharContainer extends BasePushbackContainer<CharBuffer> implements ReadableContainer<CharBuffer>, DelimitedCharContainer, ResettableContainer<CharBuffer>, PushbackContainer<CharBuffer> {

	private CyclicCharBuffer buffer;
	private String delimiter;
	private ReadableContainer<CharBuffer> parent;
	private boolean stopped = false;
	private boolean isRegex = false;
	private int delimiterSize;
	private String matchedDelimiter = null;
	private char [] stringifyBuffer;
	private String remainder;
	private String exactRegex;
	
	public BackedDelimitedCharContainer(ReadableContainer<CharBuffer> parent, int bufferSize, String delimiter) {
		this(parent, bufferSize, delimiter, delimiter.length(), false);
	}

	public BackedDelimitedCharContainer(ReadableContainer<CharBuffer> parent, int bufferSize, String regex, int delimiterSize) {
		// we need to add wildcards around the regex because we are matching a bigger container
		this(parent, bufferSize, "(?s)^.*?" + regex + ".*$", delimiterSize, true);
		this.exactRegex = regex;
	}
	
	/**
	 * You can also set a regex as delimiter but then you need to specify a buffer size because a regex is rarely fixed-length
	 * The regex could match smaller strings then the full buffer, you can turn this off by forcing matchSize to true
	 */
	private BackedDelimitedCharContainer(ReadableContainer<CharBuffer> parent, int bufferSize, String delimiter, int delimiterSize, boolean isRegex) {
		if (bufferSize < delimiterSize + 1) {
			throw new RuntimeException("The buffersize for the delimited container is too small");
		}
		this.parent = parent;
		this.delimiter = delimiter;
		this.isRegex = isRegex;
		this.buffer = new CyclicCharBuffer(bufferSize);
		this.delimiterSize = delimiterSize;
		this.stringifyBuffer = new char[bufferSize];
	}
	
	public void reinitialize(ReadableContainer<CharBuffer> parent) {
		this.buffer.truncate();
		this.parent = parent;
		this.remainder = null;
		this.stopped = false;
		this.matchedDelimiter = null;
	}
	
	@Override
	public void close() throws IOException {
		parent.close();
	}

	private String peek(CharBuffer buffer) throws IOException {
		long remainingData = buffer.remainingData();
		if (buffer.peek(IOUtils.wrap(stringifyBuffer, false)) != remainingData) {
			throw new IOException("Could not peek all content");
		}
		return new String(stringifyBuffer, 0, (int) remainingData);
	}
	
	@Override
	public long read(CharBuffer target) throws IOException {
		long amountRead = 0;
		
		if (getBuffer() != null && getBuffer().remainingData() > 0) {
			amountRead += getBuffer().read(target);
		}
		
		// if matched or stopped, return anything still in the buffer, otherwise -1
		if (matchedDelimiter != null || stopped) {
			if (buffer.remainingData() > 0) {
				amountRead += buffer.read(target);
			}
			return amountRead > 0 ? amountRead : -1;
		}

		while (target.remainingSpace() > 0) {
			// if there is only enough data in the buffer to fit the delimiter, let's read some more
			if (buffer.remainingData() <= delimiterSize) {
				long read = parent.read(buffer);
				if (read == -1) {
					stopped = true;
					break;
				}
				else if (read == 0) {
					break;
				}
			}
			String stringContent = peek(buffer);
			// check for a regex match
			if (isRegex && stringContent.matches(delimiter)) {
				// the regex may match only a part of the buffer
				// remove one character at a time from the resulting string and see when it no longer matches
				int index = 0;
				buffer.truncate();
				char [] single = new char[1];
				while(index < stringContent.length() - 1 && !stringContent.substring(index).matches("(?s)^" + exactRegex + ".*$")) {	// && !stringContent.substring(index + 1).matches(exactRegex))
					single[0] = stringContent.charAt(index);
					buffer.write(single);
					index++;
				}
				int delimiterStart = index;
				// start from the end to find the delimiter match
				while (index <= delimiterStart + delimiterSize && !stringContent.substring(delimiterStart, index + 1).matches(exactRegex)) {
					index++;
				}
				matchedDelimiter = stringContent.substring(delimiterStart, index + 1);
				remainder = getBuffer() == null ? "" : IOUtils.toString(getBuffer());
				if (index + 1 < stringContent.length()) {
					remainder += stringContent.substring(index + 1);
				}
				if (remainder.isEmpty()) {
					remainder = null;
				}
				break;
			}
			else {
				int index = stringContent.indexOf(delimiter);
				if (!isRegex && index >= 0) {
					buffer.truncate();
					// write the remainder to the target, if it can't all fit, write it to the buffer
					if (index > 0) {
						int write = (int) Math.min(index, target.remainingSpace());
						if (write > 0) {
							amountRead += target.write(stringContent.substring(0, write).toCharArray());
						}
						if (write < index) {
							buffer.write(stringContent.substring(write, index).toCharArray());
						}
					}
					matchedDelimiter = stringContent.substring(index, index + delimiterSize);
					remainder = getBuffer() == null ? "" : IOUtils.toString(getBuffer());
					if (index + delimiterSize < stringContent.length()) {
						remainder += stringContent.substring(index + delimiterSize);
					}
					if (remainder.isEmpty()) {
						remainder = null;
					}
					break;
				}
				// simply read it into the target
				else {
					amountRead += target.write(buffer.getFactory().limit(buffer, buffer.remainingData() - delimiterSize, null));
				}
			}
		}

		// if the parent has no more data and we have no more data, just return -1
		// this happens when opening a delimiting container on an empty/closed parent container
		if (amountRead == 0 && buffer.remainingData() == 0 && stopped)
			return -1;
		else
			// if the amountRead is 0 but we still have data in the buffer, return it, otherwise we will return 0 which would stop a lot of readers
			return amountRead == 0 && buffer.remainingData() > 0 ? buffer.read(target) : amountRead;
	}

	@Override
	public void reset() {
		matchedDelimiter = null;
		buffer.truncate();
		remainder = null;
		stopped = false;
		if (getBuffer() != null && getBuffer().remainingData() > 0) {
			getBuffer().truncate();
		}
	}
	
	@Override
	public boolean isDelimiterFound() {
		return matchedDelimiter != null;
	}

	@Override
	public String getMatchedDelimiter() {
		return matchedDelimiter;
	}

	public String getRemainder() {
		return remainder;
	}

}