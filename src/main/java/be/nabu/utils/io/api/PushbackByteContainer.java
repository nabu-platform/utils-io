package be.nabu.utils.io.api;

public interface PushbackByteContainer extends ReadableByteContainer {

	public void pushback(byte [] bytes);
	public void pushback(byte [] bytes, int offset, int length);
	
}