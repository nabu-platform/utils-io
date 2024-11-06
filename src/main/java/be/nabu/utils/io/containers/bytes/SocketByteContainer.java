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

package be.nabu.utils.io.containers.bytes;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

public class SocketByteContainer extends ByteChannelContainer<SocketChannel> {
	
	public SocketByteContainer(SocketAddress remote) throws IOException {
		this(null, remote);
	}
	
	public SocketByteContainer(SocketAddress local, SocketAddress remote) throws IOException {
		super(SocketChannel.open());
		getChannel().configureBlocking(false);
//		if (local != null)
//			getChannel().bind(local);
		getChannel().connect(remote);
	}
	
	public SocketByteContainer(SocketChannel channel) {
		super(channel);
	}
	
	@Override
	public boolean isReady() throws IOException {
		return getChannel().finishConnect();
	}

	@Override
	boolean isClosed() {
		return super.isClosed() || getChannel().socket().isClosed();
	}
}
