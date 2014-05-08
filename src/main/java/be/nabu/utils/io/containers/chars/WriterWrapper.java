package be.nabu.utils.io.containers.chars;

import java.io.IOException;
import java.io.Writer;

import be.nabu.utils.io.api.CharBuffer;
import be.nabu.utils.io.api.WritableContainer;

public class WriterWrapper implements WritableContainer<CharBuffer> {

	private Writer output;
	private char [] buffer = new char[4096];
	
	public WriterWrapper(Writer output) {
		this.output = output;
	}
	
	@Override
	public void close() throws IOException {
		output.close();
	}

	@Override
	public long write(CharBuffer source) throws IOException {
		long totalWritten = 0;
		while (source.remainingData() > 0) {
			int read = source.read(buffer);
			output.write(buffer, 0, read);
			totalWritten += read;
		}
		return totalWritten;
	}

	@Override
	public void flush() throws IOException {
		output.flush();
	}

}
