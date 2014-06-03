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
		getChannel().connect(remote);
	}
	
	@Override
	public boolean isReady() throws IOException {
		return getChannel().finishConnect();
	}	
}
