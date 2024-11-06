/*
* Copyright (C) 2014 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

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
		synchronized(availableDataSubscribers) {
			availableDataSubscribers.add(subscriber);
		}
		return new SubscriptionImplementation(subscriber, availableDataSubscribers);
	}

	private void fireAvailableData() {
		synchronized(availableDataSubscribers) {
			// need new arraylist because we send in the subscription specifically to allow you to unsubscribe
			for (EventfulSubscriber subscriber : new ArrayList<EventfulSubscriber>(availableDataSubscribers)) {
				try {
					subscriber.on(new SubscriptionImplementation(subscriber, availableDataSubscribers));
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public EventfulSubscription availableSpace(EventfulSubscriber subscriber) {
		synchronized(availableSpaceSubscribers) {
			availableSpaceSubscribers.add(subscriber);
		}
		return new SubscriptionImplementation(subscriber, availableSpaceSubscribers);
	}
	
	private void fireAvailableSpace() {
		synchronized(availableSpaceSubscribers) {
			for (EventfulSubscriber subscriber : new ArrayList<EventfulSubscriber>(availableSpaceSubscribers)) {
				try {
					subscriber.on(new SubscriptionImplementation(subscriber, availableSpaceSubscribers));
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public EventfulSubscription closed(EventfulSubscriber subscriber) {
		synchronized(closedSubscribers) {
			closedSubscribers.add(subscriber);
		}
		return new SubscriptionImplementation(subscriber, closedSubscribers);
	}
	
	private void fireClosed() {
		synchronized(closedSubscribers) {
			for (EventfulSubscriber subscriber : new ArrayList<EventfulSubscriber>(closedSubscribers)) {
				try {
					subscriber.on(new SubscriptionImplementation(subscriber, closedSubscribers));
				}
				catch (Exception e) {
					e.printStackTrace();
				}
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
