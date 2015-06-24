package be.nabu.utils.io.containers.bytes;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;

import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.SSLServerMode;
import be.nabu.utils.io.api.Container;

/**
 * http://docs.oracle.com/javase/7/docs/technotes/guides/security/jsse/JSSERefGuide.html#SSLENG
 */
public class SSLSocketByteContainer implements Container<be.nabu.utils.io.api.ByteBuffer> {

	private Container<be.nabu.utils.io.api.ByteBuffer> parent;
	private SSLEngine engine;
	private SSLContext context;
	private ByteBuffer application, networkIn, networkOut;
	private boolean handshakeStarted = false;
	
	private be.nabu.utils.io.api.ByteBuffer writeBuffer = IOUtils.newByteBuffer(),
			readBuffer = IOUtils.newByteBuffer();
	
	public SSLSocketByteContainer(Container<be.nabu.utils.io.api.ByteBuffer> parent, SSLContext context, SSLServerMode serverMode) throws SSLException {
		this(parent, context, false);
		switch(serverMode) {
			case WANT_CLIENT_CERTIFICATES: engine.setWantClientAuth(true); break;
			case NEED_CLIENT_CERTIFICATES: engine.setNeedClientAuth(true); break;
		}
	}
	
	public SSLSocketByteContainer(Container<be.nabu.utils.io.api.ByteBuffer> parent, SSLContext context, boolean isClient) throws SSLException {
		this.parent = parent;
		this.context = context;
		this.engine = context.createSSLEngine();
		
		engine.setUseClientMode(isClient);
		
		application = ByteBuffer.allocate(engine.getSession().getApplicationBufferSize());
		networkIn = ByteBuffer.allocate(engine.getSession().getPacketBufferSize());
		networkOut = ByteBuffer.allocate(engine.getSession().getPacketBufferSize());
	}
	
	public String [] getEnabledCipherSuites() {
		return engine.getEnabledCipherSuites();
	}
	
	public String [] getEnabledProtocols() {
		return engine.getEnabledProtocols();
	}
	
	public SSLContext getContext() {
		return context;
	}
	
	public boolean shakeHands() throws IOException {
		// start the handshake if we haven't yet
		if (!handshakeStarted) {
			engine.beginHandshake();
			handshakeStarted = true;
		}
		// the handshake status will revert to NOT_HANDSHAKING after it is finished
		while (engine.getHandshakeStatus() != HandshakeStatus.FINISHED && engine.getHandshakeStatus() != HandshakeStatus.NOT_HANDSHAKING) {
			switch(engine.getHandshakeStatus()) {
				case NEED_WRAP:
					wrap(true);
				break;
				case NEED_UNWRAP:
					unwrap(true);
				break;
				case NEED_TASK:
					// don't offload to different thread
					Runnable task = engine.getDelegatedTask();
					task.run();
				break;
				// is finished
				case FINISHED:
				case NOT_HANDSHAKING:
			}
		}
		return true;
	}
	
	private int wrap(boolean block) throws IOException {
		int totalWritten = 0;
		SSLEngineResult result;
		// first make sure we copy all data from the writeBuffer to the output
		if (writeBuffer.remainingData() == IOUtils.copyBytes(writeBuffer, parent)) {
			do {
				networkOut.clear();
				result = engine.wrap(application, networkOut);
				networkOut.flip();
				totalWritten += IOUtils.copyBytes(IOUtils.wrap(networkOut, true), block ? IOUtils.blockUntilWritten(parent, networkOut.remaining()) : parent);
				// if we can't write it to the socket, write it to the buffer
				if (networkOut.hasRemaining()) {
					writeBuffer.write(networkOut.array(), networkOut.position(), networkOut.remaining());
					break;
				}
			}
			while (result.getStatus() != SSLEngineResult.Status.OK && result.getStatus() != SSLEngineResult.Status.CLOSED);
		}
		return totalWritten;
	}
	
	private int unwrap(boolean block) throws IOException {
		int totalRead = 0;
		SSLEngineResult result;
		do {
			// there is still some room left, read more
			if (networkIn.hasRemaining())
				// only block if there is no data at all in the network buffer
				IOUtils.copyBytes(block && networkIn.remaining() == networkIn.capacity() ? IOUtils.blockUntilRead(parent) : parent, IOUtils.wrap(networkIn, false));
			// if no data has been read, stop
			if (networkIn.remaining() == networkIn.capacity())
				break;
			networkIn.flip();
			// if there is still no data, stop
			if (!networkIn.hasRemaining())
				break;
			application.clear();
			result = engine.unwrap(networkIn, application);
			application.flip();
			networkIn.compact();
			// write it all to the buffer
			totalRead += IOUtils.copyBytes(IOUtils.wrap(application, true), readBuffer);
		}
		while (result.getStatus() != SSLEngineResult.Status.OK && result.getStatus() != SSLEngineResult.Status.CLOSED);
		return totalRead;
	}
	
	@Override
	public long read(be.nabu.utils.io.api.ByteBuffer target) throws IOException {
		int readTotal = 0;
		if (shakeHands()) {
			// first try to read from the buffer
			if (readBuffer.remainingData() > 0) {
				long read = readBuffer.read(target);
				readTotal += read;
				if (readBuffer.remainingData() > 0)
					return readTotal;
			}
			while (target.remainingSpace() > 0 && readBuffer.remainingData() == 0) {
				if (engine.isInboundDone())
					return readTotal == 0 ? -1 : readTotal;
				unwrap(false);
				long read = IOUtils.copyBytes(readBuffer, target);
				if (read == 0)
					break;
				readTotal += read;
			}
		}
		return readTotal;
	}
	
	@Override
	public void close() throws IOException {
		try {
			engine.closeOutbound();
			while (!engine.isOutboundDone()) {
				application.clear();
				wrap(true);
			}
		}
		finally {
			parent.close();
		}
	}

	@Override
	public long write(be.nabu.utils.io.api.ByteBuffer source) throws IOException {
		long writeTotal = 0;
		if (shakeHands()) {
			application.clear();
			while (source.remainingData() > 0 && writeBuffer.remainingData() == 0) {
				long amount = Math.min(source.remainingData(), application.capacity());
				IOUtils.copyBytes(IOUtils.limitReadable(source, amount), IOUtils.wrap(application, false));
//				application.put(bytes, offset, amount);
				application.flip();
				wrap(true);
				application.compact();
				writeTotal += amount;
			}
		}
		return writeTotal;
	}

	@Override
	public void flush() throws IOException {
		parent.flush();
	}

}
