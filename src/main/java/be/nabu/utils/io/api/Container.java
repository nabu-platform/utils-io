package be.nabu.utils.io.api;

public interface Container<T extends Buffer<T>> extends 
	ReadableContainer<T>, 
	WritableContainer<T> {

}
