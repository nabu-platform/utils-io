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
