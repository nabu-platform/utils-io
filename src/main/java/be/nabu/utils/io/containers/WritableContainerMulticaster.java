package be.nabu.utils.io.containers;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import be.nabu.utils.io.api.Buffer;
import be.nabu.utils.io.api.WritableContainer;

/**
 * All actions will be duplicated to all outputs
 * If one or more exceptions occur, the action is first duplicated to the remaining outputs and afterwards the last exception is thrown
 * Note that the outputs have to be able to handle the input given, if you write 5 bytes, all outputs must be able to process 5 bytes
 * Use buffers if this is not guaranteed. It is otherwise rather hard to keep track of who wrote what and internal buffers may hide other problems.
 */
public class WritableContainerMulticaster<T extends Buffer<T>> implements WritableContainer<T> {

	private List<? extends WritableContainer<T>> combinedOutputs;
	
	public WritableContainerMulticaster(WritableContainer<T>...combinedOutputs) {
		this.combinedOutputs = Arrays.asList(combinedOutputs);
	}
	
	public WritableContainerMulticaster(List<? extends WritableContainer<T>> combinedOutputs) {
		this.combinedOutputs = combinedOutputs;
	}
	
	@Override
	public void close() throws IOException {
		IOException exception = null;
		for (WritableContainer<T> output : combinedOutputs) {
			try {
				output.close();
			}
			catch (IOException e) {
				exception = e;
			}
		}
		if (exception != null)
			throw exception;
	}

	@Override
	public long write(T source) throws IOException {
		// the least amount written to any output is returned
		// this means if one output is "full" and returns 0, this will be returned
		IOException lastException = null;
		T copied = source.getFactory().newInstance(source.remainingData(), true);
		for (WritableContainer<T> output : combinedOutputs) {
			source.peek(copied);
			try {
				long written = output.write(copied);
				if (written != source.remainingData())
					throw new IOException("There is not enough space in one of the outputs");
			}
			catch (IOException e) {
				lastException = e;
			}
		}
		if (lastException != null)
			throw lastException;
		return source.skip(source.remainingData());
	}

	@Override
	public void flush() throws IOException {
		IOException lastException = null;
		for (WritableContainer<T> output : combinedOutputs) {
			try {
				output.flush();
			}
			catch (IOException e) {
				lastException = e;
			}
		}
		if (lastException != null)
			throw lastException;
	}

}
