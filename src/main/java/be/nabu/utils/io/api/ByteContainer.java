package be.nabu.utils.io.api;

/**
 * A byte container can be written to and read from.
 * Because data can only enter the container by being written to it, it should always know how much data is available
 * Hence the readable part has a predictable size
 */
public interface ByteContainer extends ReadableByteContainer, WritableByteContainer {
	
}
