package be.nabu.utils.io.containers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import be.nabu.utils.io.api.Buffer;
import be.nabu.utils.io.api.Container;
import be.nabu.utils.io.api.EventfulCloseableContainer;
import be.nabu.utils.io.api.EventfulReadableContainer;
import be.nabu.utils.io.api.EventfulSubscriber;
import be.nabu.utils.io.api.EventfulSubscription;
import be.nabu.utils.io.api.EventfulWritableContainer;

public class EventfulContainerImpl<T extends Buffer<T>> implements Container<T>, EventfulReadableContainer<T>, EventfulWritableContainer<T>, EventfulCloseableContainer {

	private final class SubscriptionImplementation implements EventfulSubscription {
		private final EventfulSubscriber subscriber;
		private List<EventfulSubscriber> subscribers;

		private SubscriptionImplementation(EventfulSubscriber subscriber, List<EventfulSubscriber> subscribers) {
			this.subscriber = subscriber;
			this.subscribers = subscribers;
		}

		@Override
		public void unsubscribe() {
			synchronized(subscribers) {
				subscribers.remove(subscriber);
			}
		}
	}

	private List<EventfulSubscriber> availableDataSubscribers = new ArrayList<EventfulSubscriber>();
	private List<EventfulSubscriber> availableSpaceSubscribers = new ArrayList<EventfulSubscriber>();
	private List<EventfulSubscriber> closedSubscribers = new ArrayList<EventfulSubscriber>();
	
	private Container<T> parent;
	private boolean fireDataOnClose = true;

	public EventfulContainerImpl(Container<T> parent) {
		this.parent = parent;
	}
	
	@Override
	public long read(T buffer) throws IOException {
		long read = parent.read(buffer);
		fireAvailableSpace();
		return read;
	}

	@Override
	public void close() throws IOException {
		parent.close();
		fireClosed();
	}
	@Override
	public EventfulSubscription availableData(EventfulSubscriber subscriber) {
		availableDataSubscribers.add(subscriber);
		return new SubscriptionImplementation(subscriber, availableDataSubscribers);
	}

	private void fireAvailableData() {
		synchronized(availableDataSubscribers) {
			// need new arraylist because we send in the subscription specifically to allow you to unsubscribe
			for (EventfulSubscriber subscriber : new ArrayList<EventfulSubscriber>(availableDataSubscribers)) {
				subscriber.on(new SubscriptionImplementation(subscriber, availableDataSubscribers));
			}
		}
	}

	@Override
	public EventfulSubscription availableSpace(EventfulSubscriber subscriber) {
		availableSpaceSubscribers.add(subscriber);
		return new SubscriptionImplementation(subscriber, availableSpaceSubscribers);
	}
	
	private void fireAvailableSpace() {
		synchronized(availableSpaceSubscribers) {
			for (EventfulSubscriber subscriber : new ArrayList<EventfulSubscriber>(availableSpaceSubscribers)) {
				subscriber.on(new SubscriptionImplementation(subscriber, availableSpaceSubscribers));
			}
		}
	}

	@Override
	public EventfulSubscription closed(EventfulSubscriber subscriber) {
		closedSubscribers.add(subscriber);
		return new SubscriptionImplementation(subscriber, closedSubscribers);
	}
	
	private void fireClosed() {
		synchronized(closedSubscribers) {
			for (EventfulSubscriber subscriber : new ArrayList<EventfulSubscriber>(closedSubscribers)) {
				subscriber.on(new SubscriptionImplementation(subscriber, closedSubscribers));
			}
		}
		if (fireDataOnClose) {
			fireAvailableData();
		}
	}

	@Override
	public long write(T buffer) throws IOException {
		long write = parent.write(buffer);
		fireAvailableData();
		return write;
	}

	@Override
	public void flush() throws IOException {
		parent.flush();
		fireAvailableData();
	}

	public boolean isFireDataOnClose() {
		return fireDataOnClose;
	}

	public void setFireDataOnClose(boolean fireDataOnClose) {
		this.fireDataOnClose = fireDataOnClose;
	}
}
