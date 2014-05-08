package be.nabu.utils.io.impl;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

import be.nabu.utils.io.api.IORuntimeException;

public class SocketByteContainer extends ByteChannelContainer<SocketChannel> {
	
	public SocketByteContainer(SocketAddress remote) throws IOException {
		this(null, remote);
	}
	
	public SocketByteContainer(SocketAddress local, SocketAddress remote) throws IOException {
		super(SocketChannel.open());
		getChannel().configureBlocking(false);
		if (local != null)
			getChannel().bind(local);
		getChannel().connect(remote);
	}
	
	@Override
	public boolean isReady() {
		try {
			return getChannel().finishConnect();
		}
		catch (IOException e) {
			throw new IORuntimeException(e);
		}
	}	
}
