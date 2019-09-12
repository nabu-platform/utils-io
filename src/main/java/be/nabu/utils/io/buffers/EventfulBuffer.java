package be.nabu.utils.io.buffers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import be.nabu.utils.io.api.Buffer;
import be.nabu.utils.io.api.BufferFactory;
import be.nabu.utils.io.api.EventfulCloseableContainer;
import be.nabu.utils.io.api.EventfulReadableContainer;
import be.nabu.utils.io.api.EventfulSubscriber;
import be.nabu.utils.io.api.EventfulSubscription;
import be.nabu.utils.io.api.EventfulWritableContainer;

public class EventfulBuffer<T extends Buffer<T>> implements Buffer<T>, EventfulReadableContainer<T>, EventfulWritableContainer<T>, EventfulCloseableContainer {

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
	private T parent;

	public EventfulBuffer(T parent) {
		this.parent = parent;
	}
	
	@Override
	public long read(T buffer) throws IOException {
		long read = parent.read(buffer);
		if (parent.remainingSpace() > 0) {
			fireAvailableSpace();
		}
		return read;
	}

	@Override
	public void close() throws IOException {
		parent.close();		
		fireClosed();
	}

	@Override
	public long write(T buffer) throws IOException {
		long write = parent.write(buffer);
		if (parent.remainingData() > 0) {
			fireAvailableData();
		}
		return write;
	}

	@Override
	public void flush() throws IOException {
		parent.flush();
	}

	@Override
	public long remainingData() {
		return parent.remainingData();
	}

	@Override
	public long remainingSpace() {
		return parent.remainingSpace();
	}

	@Override
	public void truncate() {
		parent.truncate();
	}

	@Override
	public long skip(long amount) throws IOException {
		return parent.skip(amount);
	}

	@Override
	public long peek(T buffer) throws IOException {
		return parent.peek(buffer);
	}

	@Override
	public BufferFactory<T> getFactory() {
		return parent.getFactory();
	}

	@Override
	public EventfulSubscription availableData(EventfulSubscriber subscriber) {
		availableDataSubscribers.add(subscriber);
		return new SubscriptionImplementation(subscriber, availableDataSubscribers);
	}

	private void fireAvailableData() {
		synchronized(availableDataSubscribers) {
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
	}

}
