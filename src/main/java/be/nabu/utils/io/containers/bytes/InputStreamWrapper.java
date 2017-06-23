package be.nabu.utils.io.containers.bytes;

import java.io.IOException;
import java.io.InputStream;

import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;

public class InputStreamWrapper implements ReadableContainer<ByteBuffer> {

	private InputStream input;
	private byte [] buffer = new byte[4096];
	private boolean closed;
	
	public InputStreamWrapper(InputStream input) {
		this.input = input;
	}
	
	@Override
	public void close() throws IOException {
		input.close();
	}

	@Override
	public long read(ByteBuffer target) throws IOException {
		long totalRead = 0;
		while (!closed && target.remainingSpace() > 0) {
			int requestedData = (int) Math.min(buffer.length, target.remainingSpace());
			int read = input.read(buffer, 0, requestedData);
			if (read < 0) {
				closed = true;
				break;
			}
			else
				totalRead += read;
			target.write(buffer, 0, read);
			// if you read less than you wanted, it may indicate the inputstream has no data left
			// if we keep going we might hang, instead we break
			// this gives the calling code the choice if reading again or stopping
			// for example in some cases you know how much data you have to read
			if (read < requestedData)
				break;
		}
		return totalRead == 0 && closed ? -1 : totalRead;
	}

}
