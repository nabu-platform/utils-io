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
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SocketChannel;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;
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
	private ByteBuffer applicationIn, applicationOut, networkIn, networkOut;
	private Date handshakeStarted, handshakeStopped;
	private Long handshakeTimeout, readTimeout;
	private boolean isStartTls;
	
	private be.nabu.utils.io.api.ByteBuffer writeBuffer = IOUtils.newByteBuffer(),
			readBuffer = IOUtils.newByteBuffer();
	
	private boolean isClosed;
	private boolean isClient;
	private String hostName;
	
	public SSLSocketByteContainer(Container<be.nabu.utils.io.api.ByteBuffer> parent, SSLContext context, SSLServerMode serverMode) throws SSLException {
		this(parent, context, false);
		switch(serverMode) {
			case WANT_CLIENT_CERTIFICATES: engine.setWantClientAuth(true); break;
			case NEED_CLIENT_CERTIFICATES: engine.setNeedClientAuth(true); break;
			case NO_CLIENT_CERTIFICATES: // do nothing
		}
	}
	
	public SSLSocketByteContainer(Container<be.nabu.utils.io.api.ByteBuffer> parent, SSLContext context, boolean isClient) throws SSLException {
		this(parent, context, isClient, null);
	}
	
	public SSLSocketByteContainer(Container<be.nabu.utils.io.api.ByteBuffer> parent, SSLContext context, boolean isClient, String hostName) throws SSLException {
		this.parent = parent;
		this.context = context;
		this.isClient = isClient;
		this.hostName = hostName;
		this.engine = context.createSSLEngine();
		
		engine.setUseClientMode(isClient);
		
		applicationIn = ByteBuffer.allocate(engine.getSession().getApplicationBufferSize());
		applicationOut = ByteBuffer.allocate(engine.getSession().getApplicationBufferSize() / 2);
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
			if (isClient) {
				if (hostName == null) {
					SocketAddress remoteAddress = null;
					if (this.parent instanceof ByteChannelContainer) {
						ByteChannel channel = ((ByteChannelContainer<?>) parent).getChannel();
						if (channel instanceof SocketChannel) {
							remoteAddress = ((SocketChannel) channel).socket().getRemoteSocketAddress();
						}
					}
					if (remoteAddress instanceof InetSocketAddress) {
						hostName = ((InetSocketAddress) remoteAddress).getHostName();
					}
				}
				// add support for SNI
				if (hostName != null) {
					List<SNIServerName> serverNames = new ArrayList<SNIServerName>();
					serverNames.add(new SNIHostName(hostName));
					SSLParameters sslParameters = new SSLParameters();
					sslParameters.setServerNames(serverNames);
					engine.setSSLParameters(sslParameters);
				}
			}
			engine.beginHandshake();
			handshakeStarted = new Date();
		}
		// the handshake status will revert to NOT_HANDSHAKING after it is finished
		handshake: while (!isClosed && engine.getHandshakeStatus() != HandshakeStatus.FINISHED && engine.getHandshakeStatus() != HandshakeStatus.NOT_HANDSHAKING) {
			if (new Date().getTime() > handshakeStarted.getTime() + getHandshakeTimeout()) {
				handshakeStopped = new Date();
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
					if (task != null) {
						//ForkJoinPool.commonPool().submit(task);
						task.run();
					}
				break;
				// is finished
				case FINISHED:
				case NOT_HANDSHAKING:
			}
		}
		if (handshakeStopped == null && !isClosed) {
			handshakeStopped = new Date();
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
				result = engine.wrap(applicationOut, networkOut);
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
				ReadableContainer<be.nabu.utils.io.api.ByteBuffer> readable = block && networkIn.remaining() == networkIn.capacity() ? IOUtils.blockUntilRead(parent, 1, getReadTimeout()) : parent;
				long read = readable.read(new NioByteBufferWrapper(networkIn, false));
				if (read < 0) {
					isClosed = true;
				}
				// if we read no new data and we were already in an underflow situation, we should stop trying to read more
				if (!block && read == 0 && result != null && result.getStatus().equals(SSLEngineResult.Status.BUFFER_UNDERFLOW)) {
					break;
				}
			}
			// if no data has been read, stop
			if (networkIn.remaining() == networkIn.capacity())
				break;
			networkIn.flip();
			// if there is still no data, stop
			if (!networkIn.hasRemaining())
				break;
			applicationIn.clear();
			result = engine.unwrap(networkIn, applicationIn);
			applicationIn.flip();
			networkIn.compact();
			// write it all to the buffer
			totalRead += IOUtils.wrap(applicationIn, true).read(readBuffer);
			if (applicationIn.remaining() != 0) {
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
				applicationOut.clear();
				wrap(true);
			}
			
			// NOT ACTIVE: check the TLS 1.3 notes in the changes folder
//			engine.closeInbound();
//			while (!engine.isInboundDone()) {
//				applicationIn.clear();
//				unwrap(true);
//			}
		}
		finally {
			isClosed = true;
			parent.close();
		}
	}

	@Override
	public long write(be.nabu.utils.io.api.ByteBuffer source) throws IOException {
		// if we are in start tls mode, the handshake (which is initiated by the client) is NOT started yet
		// and the server wants to write something out, do this in plain text, it is probably the confirmation that TLS can be started
		if (isStartTls && !isClient && handshakeStarted == null) {
			return parent.write(source);
		}
		long writeTotal = 0;
		if (!isClosed && shakeHands()) {
			while (source.remainingData() > 0 && writeBuffer.remainingData() == 0) {
				long amount = Math.min(source.remainingData(), applicationOut.remaining());
				IOUtils.copyBytes(IOUtils.limitReadable(source, amount), IOUtils.wrap(applicationOut, false));
//				application.put(bytes, offset, amount);
				applicationOut.flip();
				if (wrap(true) == -1) {
					isClosed = true;
					break;
				}
				applicationOut.compact();
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
	
	public Long getReadTimeout() {
		if (readTimeout == null) {
			// it is possible to generate a read timeout between frames because of the "block until read"
			// this is the timeout for how long it will wait, 15 seconds by default
			readTimeout = Long.parseLong(System.getProperty("ssl.read.timeout", "15000"));
		}
		return readTimeout;
	}

	public void setReadTimeout(Long readTimeout) {
		this.readTimeout = readTimeout;
	}

	public boolean isStartTls() {
		return isStartTls;
	}

	public void setStartTls(boolean isStartTls) {
		this.isStartTls = isStartTls;
	}
	
	public Long getHandshakeDuration() {
		return handshakeStarted != null && handshakeStopped != null ? handshakeStopped.getTime() - handshakeStarted.getTime() : null;
	}
}
