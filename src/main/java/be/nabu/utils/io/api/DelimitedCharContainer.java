package be.nabu.utils.io.api;

public interface DelimitedCharContainer extends ReadableContainer<CharBuffer> {
	public boolean isDelimiterFound();
	public String getMatchedDelimiter();
}
