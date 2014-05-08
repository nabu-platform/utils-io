package be.nabu.utils.io.impl;

import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.IORuntimeException;
import be.nabu.utils.io.api.WritableCharContainer;

/**
 * All actions will be duplicated to all outputs
 * If one or more exceptions occur, the action is first duplicated to the remaining outputs and afterwards the last exception is thrown
 */
public class WritableCharContainerCombiner implements WritableCharContainer {

	private WritableCharContainer [] combinedOutputs;
	
	public WritableCharContainerCombiner(WritableCharContainer...combinedOutputs) {
		this.combinedOutputs = combinedOutputs;
	}
	
	@Override
	public void close() {
		IOUtils.close(combinedOutputs);
	}

	@Override
	public int write(char[] chars) {
		return write(chars, 0, chars.length);
	}

	@Override
	public int write(char[] chars, int offset, int length) {
		// the least amount written to any output is returned
		// this means if one output is "full" and returns 0, this will be returned
		int minWritten = length;
		IORuntimeException lastException = null;
		for (WritableCharContainer output : combinedOutputs) {
			try {
				int written = output.write(chars, offset, length);
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
		for (WritableCharContainer output : combinedOutputs) {
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
