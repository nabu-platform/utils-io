package be.nabu.utils.io.containers.bytes;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.cert.Certificate;
import java.util.Date;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;

import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.SSLServerMode;
import be.nabu.utils.io.api.Container;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.api.WritableContainer;
import be.nabu.utils.io.buffers.bytes.NioByteBufferWrapper;

/**
 * http://docs.oracle.com/javase/7/docs/technotes/guides/security/jsse/JSSERefGuide.html#SSLENG
 * 
 * Note that currently the handshake _is_ blocking but has a configurable timeout (defaults to 30 seconds)
 */
public class SSLSocketByteContainer implements Container<be.nabu.utils.io.api.ByteBuffer> {

	private Container<be.nabu.utils.io.api.ByteBuffer> parent;
	private SSLEngine engine;
	private SSLContext context;
	private ByteBuffer application, networkIn, networkOut;
	private Date handshakeStarted;
	private Long handshakeTimeout;
	
	private be.nabu.utils.io.api.ByteBuffer writeBuffer = IOUtils.newByteBuffer(),
			readBuffer = IOUtils.newByteBuffer();
	
	private boolean isClosed;
	
	public SSLSocketByteContainer(Container<be.nabu.utils.io.api.ByteBuffer> parent, SSLContext context, SSLServerMode serverMode) throws SSLException {
		this(parent, context, false);
		switch(serverMode) {
			case WANT_CLIENT_CERTIFICATES: engine.setWantClientAuth(true); break;
			case NEED_CLIENT_CERTIFICATES: engine.setNeedClientAuth(true); break;
			case NO_CLIENT_CERTIFICATES: // do nothing
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
	
	public Certificate[] getPeerCertificates() {
		try {
			return engine.getSession().getPeerCertificates();
		}
		catch (SSLPeerUnverifiedException e) {
			return null;
		}
	}
	
	public boolean shakeHands() throws IOException {
		// start the handshake if we haven't yet
		if (handshakeStarted == null) {
			engine.beginHandshake();
			handshakeStarted = new Date();
		}
		// the handshake status will revert to NOT_HANDSHAKING after it is finished
		handshake: while (engine.getHandshakeStatus() != HandshakeStatus.FINISHED && engine.getHandshakeStatus() != HandshakeStatus.NOT_HANDSHAKING) {
			if (new Date().getTime() > handshakeStarted.getTime() + getHandshakeTimeout()) {
				throw new SSLException("Handshaked timed out");
			}
			switch(engine.getHandshakeStatus()) {
				case NEED_WRAP:
					if (wrap(true) == -1) {
						isClosed = true;
						break handshake;
					}
				break;
				case NEED_UNWRAP:
					if (unwrap(true) == -1) {
						isClosed = true;
						break handshake;
					}
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
		return !isClosed;
	}
	
	@SuppressWarnings("resource")
	private int wrap(boolean block) throws IOException {
		int totalWritten = 0;
		SSLEngineResult result = null;
		long write = parent.write(writeBuffer);
		isClosed |= write == -1;
		// first make sure we copy all data from the writeBuffer to the output
		if (write >= 0 && writeBuffer.remainingData() == 0) {
			do {
				networkOut.clear();
				result = engine.wrap(application, networkOut);
				networkOut.flip();
				WritableContainer<be.nabu.utils.io.api.ByteBuffer> writable = block ? IOUtils.blockUntilWritten(parent, networkOut.remaining()) : parent;
				write = writable.write(new NioByteBufferWrapper(networkOut, true));
				if (write == -1) {
					isClosed = true;
				}
				else {
					totalWritten += write;
				}
				// if we can't write it to the socket, write it to the buffer
				if (networkOut.hasRemaining()) {
					writeBuffer.write(networkOut.array(), networkOut.position(), networkOut.remaining());
					break;
				}
			}
			while (!isClosed && result.getStatus() != SSLEngineResult.Status.OK && result.getStatus() != SSLEngineResult.Status.CLOSED);
		}
		isClosed |= result != null && result.getStatus() == SSLEngineResult.Status.CLOSED; 
		return totalWritten == 0 && isClosed ? -1 : totalWritten;
	}
	
	@SuppressWarnings("resource")
	private int unwrap(boolean block) throws IOException {
		int totalRead = 0;
		SSLEngineResult result = null;
		do {
			// there is still some room left, read more
			if (networkIn.hasRemaining()) {
				// only block if there is no data at all in the network buffer
				ReadableContainer<be.nabu.utils.io.api.ByteBuffer> readable = block && networkIn.remaining() == networkIn.capacity() ? IOUtils.blockUntilRead(parent) : parent;
				long read = readable.read(new NioByteBufferWrapper(networkIn, false));
				if (read == -1) {
					isClosed = true;
				}
			}
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
			totalRead += IOUtils.wrap(application, true).read(readBuffer);
			if (application.remaining() != 0) {
				throw new IOException("Could not copy the application buffer to the target read buffer");
			}
			//IOUtils.copyBytes(IOUtils.wrap(application, true), readBuffer);
		}
		while (!isClosed && result.getStatus() != SSLEngineResult.Status.OK && result.getStatus() != SSLEngineResult.Status.CLOSED);
		isClosed |= result != null && result.getStatus() == SSLEngineResult.Status.CLOSED;
		return totalRead == 0 && isClosed ? -1 : totalRead;
	}
	
	@Override
	public long read(be.nabu.utils.io.api.ByteBuffer target) throws IOException {
		int readTotal = 0;
		if (!isClosed && shakeHands()) {
			// first try to read from the buffer
			if (readBuffer.remainingData() > 0) {
				long read = readBuffer.read(target);
				readTotal += read;
				if (readBuffer.remainingData() > 0)
					return readTotal;
			}
			long read = 0;
			while (target.remainingSpace() > 0 && readBuffer.remainingData() == 0) {
				if (engine.isInboundDone()) {
					isClosed = true;
					break;
				}
				read = unwrap(false);
				if (read == -1) {
					isClosed = true;
				}
				read = target.write(readBuffer);
				if (read <= 0)
					break;
				readTotal += read;
			}
		}
		return readTotal == 0 && isClosed ? -1 : readTotal;
	}
	
	@Override
	public void close() throws IOException {
		try {
			engine.closeOutbound();
			while (!isClosed && !engine.isOutboundDone()) {
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
		if (!isClosed && shakeHands()) {
			application.clear();
			while (source.remainingData() > 0 && writeBuffer.remainingData() == 0) {
				long amount = Math.min(source.remainingData(), application.capacity());
				IOUtils.copyBytes(IOUtils.limitReadable(source, amount), IOUtils.wrap(application, false));
//				application.put(bytes, offset, amount);
				application.flip();
				if (wrap(true) == -1) {
					isClosed = true;
					break;
				}
				application.compact();
				writeTotal += amount;
			}
		}
		return writeTotal == 0 && isClosed ? -1 : writeTotal;
	}

	@Override
	public void flush() throws IOException {
		parent.flush();
	}

	public Long getHandshakeTimeout() {
		if (handshakeTimeout == null) {
			// 30 seconds to time out should be enough for a handshake?
			handshakeTimeout = Long.parseLong(System.getProperty("ssl.handshake.timeout", "30000"));
		}
		return handshakeTimeout;
	}

	public void setHandshakeTimeout(Long handshakeTimeout) {
		this.handshakeTimeout = handshakeTimeout;
	}
}
