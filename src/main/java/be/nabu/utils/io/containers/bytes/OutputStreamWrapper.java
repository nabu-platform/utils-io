package be.nabu.utils.io.containers.bytes;

import java.io.IOException;
import java.io.OutputStream;

import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.WritableContainer;

public class OutputStreamWrapper implements WritableContainer<ByteBuffer> {

	private OutputStream output;
	private byte [] buffer = new byte[4096];
	
	public OutputStreamWrapper(OutputStream output) {
		this.output = output;
	}
	
	@Override
	public void close() throws IOException {
		output.close();
	}

	@Override
	public long write(ByteBuffer source) throws IOException {
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
