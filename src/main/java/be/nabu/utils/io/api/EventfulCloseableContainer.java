package be.nabu.utils.io.api;

public interface EventfulCloseableContainer {
	public EventfulSubscription closed(EventfulSubscriber subscriber);
}
