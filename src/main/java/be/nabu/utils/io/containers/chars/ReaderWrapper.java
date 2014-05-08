package be.nabu.utils.io.containers.chars;

import java.io.IOException;
import java.io.Reader;

import be.nabu.utils.io.api.CharBuffer;
import be.nabu.utils.io.api.ReadableContainer;

public class ReaderWrapper implements ReadableContainer<CharBuffer> {

	private Reader input;
	private char [] buffer = new char[4096];
	
	public ReaderWrapper(Reader input) {
		this.input = input;
	}
	
	@Override
	public void close() throws IOException {
		input.close();
	}

	@Override
	public long read(CharBuffer target) throws IOException {
		long totalRead = 0;
		while (target.remainingSpace() > 0) {
			int read = input.read(buffer, 0, (int) Math.min(buffer.length, target.remainingSpace()));
			if (read == -1) {
				if (totalRead == 0)
					totalRead = -1;
				break;
			}
			target.write(buffer, 0, read);
		}
		return totalRead;
	}

}
