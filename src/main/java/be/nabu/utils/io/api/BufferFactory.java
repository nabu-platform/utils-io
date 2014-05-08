package be.nabu.utils.io.api;


public interface BufferFactory<T extends Buffer<T>> {
	public T newInstance(long size, boolean cyclic);
	public T newInstance();
	public T newSink(long size);
}
