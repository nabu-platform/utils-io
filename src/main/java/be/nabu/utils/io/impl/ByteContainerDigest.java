package be.nabu.utils.io.impl;

import java.io.IOException;
import java.security.MessageDigest;

import be.nabu.utils.io.api.ByteContainer;
import be.nabu.utils.io.api.WritableByteContainer;

public class ByteContainerDigest implements ByteContainer {

	private boolean closed = false;
	private DynamicByteContainer result = new DynamicByteContainer();
	
	private WritableByteContainer chainedOutput;
	private MessageDigest digest;
	
	public ByteContainerDigest(MessageDigest digest, WritableByteContainer chainedOutput) {
		this.digest = digest;
		this.chainedOutput = chainedOutput;
	}
	
	@Override
	public int read(byte[] bytes) {
		return read(bytes, 0, bytes.length);
	}

	@Override
	public int read(byte[] bytes, int offset, int length) {
		if (!closed)
			return 0;
		else
			return result.read(bytes, offset, length);
	}

	@Override
	public void close() throws IOException {
		try {
			chainedOutput.close();
		}
		finally {
			result.write(digest.digest());
			result.close();
			closed = true;
		}
	}

	@Override
	public int write(byte[] bytes) {
		digest.update(bytes);
		return chainedOutput.write(bytes);
	}

	@Override
	public int write(byte[] bytes, int offset, int length) {
		digest.update(bytes, offset, length);
		return chainedOutput.write(bytes, offset, length);
	}

	@Override
	public void flush() {
		chainedOutput.flush();
	}
}
