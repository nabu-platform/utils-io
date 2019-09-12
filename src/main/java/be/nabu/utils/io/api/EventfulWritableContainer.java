package be.nabu.utils.io.api;

public interface EventfulWritableContainer<T extends Buffer<T>> extends WritableContainer<T> {
	public EventfulSubscription availableSpace(EventfulSubscriber subscriber);
}
