package be.nabu.utils.io.api;

public interface PushbackCharContainer extends ReadableCharContainer {

	public void pushback(char [] chars);
	public void pushback(char [] chars, int offset, int length);
	
}