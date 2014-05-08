package be.nabu.utils.io.impl;

import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.IORuntimeException;
import be.nabu.utils.io.api.WritableByteContainer;

/**
 * All actions will be duplicated to all outputs
 * If one or more exceptions occur, the action is first duplicated to the remaining outputs and afterwards the last exception is thrown
 */
public class WritableByteContainerCombiner implements WritableByteContainer {

	private WritableByteContainer [] combinedOutputs;
	
	public WritableByteContainerCombiner(WritableByteContainer...combinedOutputs) {
		this.combinedOutputs = combinedOutputs;
	}
	
	@Override
	public void close() {
		IOUtils.close(combinedOutputs);
	}

	@Override
	public int write(byte[] bytes) {
		return write(bytes, 0, bytes.length);
	}

	@Override
	public int write(byte[] bytes, int offset, int length) {
		// the least amount written to any output is returned
		// this means if one output is "full" and returns 0, this will be returned
		int minWritten = length;
		IORuntimeException lastException = null;
		for (WritableByteContainer output : combinedOutputs) {
			try {
				int written = output.write(bytes, offset, length);
				if (written < minWritten)
					minWritten = written;
			}
			catch (IORuntimeException e) {
				lastException = e;
			}
		}
		if (lastException != null)
			throw lastException;
		return minWritten;
	}

	@Override
	public void flush() {
		IORuntimeException lastException = null;
		for (WritableByteContainer output : combinedOutputs) {
			try {
				output.flush();
			}
			catch (IORuntimeException e) {
				lastException = e;
			}
		}
		if (lastException != null)
			throw lastException;
	}

}
