package be.nabu.utils.io.api;

public interface EventfulReadableContainer<T extends Buffer<T>> extends ReadableContainer<T> {
	public EventfulSubscription availableData(EventfulSubscriber subscriber);
}
