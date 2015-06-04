package be.nabu.utils.io.buffers.chars;

import java.io.IOException;

import be.nabu.utils.io.api.BufferFactory;
import be.nabu.utils.io.api.CharBuffer;

/**
 * Note that it is usually a _lot_ faster to simply convert to char array and use StaticCharBuffer
 * Tests indicate that StaticCharBuffer is 6 times faster for a string converted to char[] than wrapping this directly around the string even though the string to char is a copy
 */
public class CharSequenceBuffer implements CharBuffer {

	private CharSequence sequence;
	private int pointer;
	private char[] single = new char[1];

	public CharSequenceBuffer(CharSequence sequence) {
		this.sequence = sequence;
	}
	
	@Override
	public BufferFactory<CharBuffer> getFactory() {
		return CharBufferFactory.getInstance();
	}

	@Override
	public long read(CharBuffer buffer) throws IOException {
		return read(buffer, false);
	}
	@Override
	public int read(char[] chars, int offset, int length) throws IOException {
		int total = 0;
		while (length > 0 && pointer < sequence.length()) {
			chars[offset++] = sequence.charAt(pointer++);
			length--;
		}
		return total == 0 && length > 0 ? -1 : total;
	}

	@Override
	public int read(char[] chars) throws IOException {
		return read(chars, 0, chars.length);
	}

	private long read(CharBuffer buffer, boolean peek) throws IOException {
		long total = 0;
		int pointer = this.pointer;
		while (buffer.remainingSpace() > 0 && pointer < sequence.length()) {
			single[0] = sequence.charAt(pointer++);
			buffer.write(single);
		}
		if (!peek) {
			this.pointer = pointer;
		}
		return total == 0 && buffer.remainingSpace() > 0 ? -1 : total;
	}

	@Override
	public void close() throws IOException {
		// do nothing
	}

	@Override
	public void flush() throws IOException {
		// do nothing
	}

	@Override
	public long remainingData() {
		return sequence.length() - pointer;
	}

	@Override
	public long remainingSpace() {
		return 0;
	}

	@Override
	public void truncate() {
		pointer = sequence.length();
	}

	@Override
	public long skip(long amount) throws IOException {
		int newPointer = (int) Math.min(sequence.length(), pointer + amount);
		int skipAmount = newPointer - pointer;
		pointer = newPointer;
		return skipAmount;
	}

	@Override
	public long peek(CharBuffer buffer) throws IOException {
		return read(buffer, true);
	}

	@Override
	public long write(CharBuffer buffer) throws IOException {
		throw new IOException("The charsequence is closed");
	}
	@Override
	public int write(char[] chars, int offset, int length) throws IOException {
		throw new IOException("The charsequence is closed");
	}
	@Override
	public int write(char[] chars) throws IOException {
		throw new IOException("The charsequence is closed");
	}

}
