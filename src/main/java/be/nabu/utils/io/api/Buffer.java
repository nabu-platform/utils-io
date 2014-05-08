package be.nabu.utils.io.api;


public interface Buffer<T extends Buffer<T>> extends 
		Container<T>, 
		LimitedReadableContainer<T>,
		LimitedWritableContainer<T>,
		TruncatableContainer<T>,
		SkippableContainer<T>,
		PeekableContainer<T> {

	public BufferFactory<T> getFactory();
	
}
