package be.nabu.utils.io.impl;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;

import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteContainer;
import be.nabu.utils.io.api.IORuntimeException;

/**
 * http://docs.oracle.com/javase/7/docs/technotes/guides/security/jsse/JSSERefGuide.html#SSLENG
 */
public class SSLSocketByteContainer implements ByteContainer {

	private ByteContainer parent;
	private SSLEngine engine;
	private SSLContext context;
	private ByteBuffer application, networkIn, networkOut;
	private boolean handshakeStarted = false;
	
	private DynamicByteContainer writeBuffer = new DynamicByteContainer(),
			readBuffer = new DynamicByteContainer();
	
	public SSLSocketByteContainer(ByteContainer parent, SSLContext context, boolean isClient) throws SSLException {
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
	
	@Override
	public int read(byte[] bytes) {
		return read(bytes, 0, bytes.length);
	}
	
	public boolean shakeHands() {
		try {
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
		}
		catch (SSLException e) {
			throw new IORuntimeException(e);
		}
		return true;
	}
	
	private int wrap(boolean block) {
		int totalWritten = 0;
		SSLEngineResult result;
		// first make sure we copy all data from the writeBuffer to the output
		if (writeBuffer.remainingData() == IOUtils.copy(writeBuffer, parent)) {
			do {
				networkOut.clear();
				try {
					result = engine.wrap(application, networkOut);
				}
				catch (SSLException e) {
					throw new IORuntimeException(e);
				}
				networkOut.flip();
				totalWritten += IOUtils.copy(IOUtils.wrap(networkOut, true), block ? IOUtils.blockUntilWritten(parent, networkOut.remaining()) : parent);
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
	
	private int unwrap(boolean block) {
		int totalRead = 0;
		SSLEngineResult result;
		do {
			// there is still some room left, read more
			if (networkIn.hasRemaining())
				// only block if there is no data at all in the network buffer
				IOUtils.copy(block && networkIn.remaining() == networkIn.capacity() ? IOUtils.blockUntilRead(parent) : parent, IOUtils.wrap(networkIn, false));
			// if no data has been read, stop
			if (networkIn.remaining() == networkIn.capacity())
				break;
			networkIn.flip();
			// if there is still no data, stop
			if (!networkIn.hasRemaining())
				break;
			application.clear();
			try {
				result = engine.unwrap(networkIn, application);
			}
			catch (SSLException e) {
				throw new IORuntimeException(e);
			}
			application.flip();
			networkIn.compact();
			// write it all to the buffer
			totalRead += IOUtils.copy(IOUtils.wrap(application, true), readBuffer);
		}
		while (result.getStatus() != SSLEngineResult.Status.OK && result.getStatus() != SSLEngineResult.Status.CLOSED);
		return totalRead;
	}
	
	@Override
	public int read(byte[] bytes, int offset, int length) {
		int readTotal = 0;
		if (shakeHands()) {
			// first try to read from the buffer
			if (readBuffer.remainingData() > 0) {
				int read = readBuffer.read(bytes, offset, length);
				length -= read;
				offset += read;
				readTotal += read;
				if (readBuffer.remainingData() > 0)
					return readTotal;
			}
			while (length > 0 && readBuffer.remainingData() == 0) {
				if (engine.isInboundDone())
					return readTotal == 0 ? -1 : readTotal;
				unwrap(false);
				long read = IOUtils.copy(readBuffer, IOUtils.wrap(bytes, offset, length, false));
				if (read == 0)
					break;
				readTotal += read;
				length -= read;
				offset += read;
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
	public int write(byte[] bytes) {
		return write(bytes, 0, bytes.length);
	}

	@Override
	public int write(byte[] bytes, int offset, int length) {
		int writeTotal = 0;
		if (shakeHands()) {
			application.clear();
			while (length > 0 && writeBuffer.remainingData() == 0) {
				int amount = Math.min(length, application.capacity());
				application.put(bytes, offset, amount);
				application.flip();
				wrap(true);
				application.compact();
				length -= amount;
				writeTotal += amount;
				offset += amount;
			}
		}
		return writeTotal;
	}

	@Override
	public void flush() {
		parent.flush();
	}

}
