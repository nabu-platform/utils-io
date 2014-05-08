package be.nabu.utils.io;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.SocketOption;
import java.nio.channels.ByteChannel;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;

import be.nabu.utils.io.api.Buffer;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.CharBuffer;
import be.nabu.utils.io.api.Container;
import be.nabu.utils.io.api.CountingReadableContainer;
import be.nabu.utils.io.api.CountingWritableContainer;
import be.nabu.utils.io.api.DelimitedCharContainer;
import be.nabu.utils.io.api.LimitedReadableContainer;
import be.nabu.utils.io.api.LimitedWritableContainer;
import be.nabu.utils.io.api.MarkableContainer;
import be.nabu.utils.io.api.PeekableContainer;
import be.nabu.utils.io.api.PushbackContainer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.api.WritableContainer;
import be.nabu.utils.io.buffers.bytes.ByteBufferSink;
import be.nabu.utils.io.buffers.bytes.CyclicByteBuffer;
import be.nabu.utils.io.buffers.bytes.DynamicByteBuffer;
import be.nabu.utils.io.buffers.bytes.NioByteBufferWrapper;
import be.nabu.utils.io.buffers.bytes.StaticByteBuffer;
import be.nabu.utils.io.buffers.chars.CharBufferSink;
import be.nabu.utils.io.buffers.chars.CyclicCharBuffer;
import be.nabu.utils.io.buffers.chars.DynamicCharBuffer;
import be.nabu.utils.io.buffers.chars.NioCharBufferWrapper;
import be.nabu.utils.io.buffers.chars.StaticCharBuffer;
import be.nabu.utils.io.containers.BlockingReadableContainer;
import be.nabu.utils.io.containers.BlockingWritableContainer;
import be.nabu.utils.io.containers.BufferedReadableContainer;
import be.nabu.utils.io.containers.BufferedWritableContainer;
import be.nabu.utils.io.containers.ComposedContainer;
import be.nabu.utils.io.containers.CountingReadableContainerImpl;
import be.nabu.utils.io.containers.CountingWritableContainerImpl;
import be.nabu.utils.io.containers.FixedLengthReadableContainer;
import be.nabu.utils.io.containers.LimitedMarkableContainer;
import be.nabu.utils.io.containers.LimitedReadableContainerImpl;
import be.nabu.utils.io.containers.LimitedWritableContainerImpl;
import be.nabu.utils.io.containers.MinimalReadableContainer;
import be.nabu.utils.io.containers.MinimalWritableContainer;
import be.nabu.utils.io.containers.PushbackContainerImpl;
import be.nabu.utils.io.containers.ReadableContainerChainer;
import be.nabu.utils.io.containers.SynchronizedReadableContainer;
import be.nabu.utils.io.containers.SynchronizedWritableContainer;
import be.nabu.utils.io.containers.WritableContainerMulticaster;
import be.nabu.utils.io.containers.bytes.ByteChannelContainer;
import be.nabu.utils.io.containers.bytes.ByteContainerDigest;
import be.nabu.utils.io.containers.bytes.ByteContainerInputStream;
import be.nabu.utils.io.containers.bytes.ByteContainerOutputStream;
import be.nabu.utils.io.containers.bytes.FileWrapper;
import be.nabu.utils.io.containers.bytes.InputStreamWrapper;
import be.nabu.utils.io.containers.bytes.OutputStreamWrapper;
import be.nabu.utils.io.containers.bytes.ReadableCharToByteContainer;
import be.nabu.utils.io.containers.bytes.SSLSocketByteContainer;
import be.nabu.utils.io.containers.bytes.SocketByteContainer;
import be.nabu.utils.io.containers.chars.BackedReadableCharContainer;
import be.nabu.utils.io.containers.chars.BackedWritableCharContainer;
import be.nabu.utils.io.containers.chars.CharContainerReader;
import be.nabu.utils.io.containers.chars.CharContainerWriter;
import be.nabu.utils.io.containers.chars.DelimitedCharContainerImpl;
import be.nabu.utils.io.containers.chars.ReaderWrapper;
import be.nabu.utils.io.containers.chars.ValidatedReadableCharContainer;
import be.nabu.utils.io.containers.chars.WriterWrapper;

public class IOUtils {
	
	public static byte [] toBytes(ReadableContainer<ByteBuffer> container) throws IOException {
		int size = 0;
		// if we can't determine the size, we first have to copy it to a sized container
		if (container instanceof LimitedReadableContainer)
			size = (int) ((LimitedReadableContainer<ByteBuffer>) container).remainingData();
		else {
			DynamicByteBuffer dynamicBuffer = new DynamicByteBuffer();
			size = (int) copyBytes(container, dynamicBuffer);
			container = dynamicBuffer;
		}
		byte [] bytes = new byte[size];
		StaticByteBuffer buffer = new StaticByteBuffer(bytes, false);
		long copied = 0;
		// this should only happen if you are multithreading
		if ((copied = copyBytes(container, buffer)) != bytes.length)
			throw new IOException("Could only copy " + copied + "/" + bytes.length + " of the data");
		return bytes;
	}
	
	public static String toString(ReadableContainer<CharBuffer> container) throws IOException {
		return new String(toChars(container));
	}

	public static <T extends Buffer<T>> ReadableContainer<T> blockUntilRead(ReadableContainer<T> input, long timeout, TimeUnit timeUnit) {
		return new BlockingReadableContainer<T>(input, timeout, timeUnit);
	}
	
	public static <T extends Buffer<T>> WritableContainer<T> blockUntilWritten(WritableContainer<T> output, long timeout, TimeUnit timeUnit) {
		return new BlockingWritableContainer<T>(output, timeout, timeUnit);
	}
	
	public static char [] toChars(ReadableContainer<CharBuffer> container) throws IOException {
		int size = 0;
		// if we can't determine the size, we first have to copy it to a sized container
		if (container instanceof LimitedReadableContainer)
			size = (int) ((LimitedReadableContainer<CharBuffer>) container).remainingData();
		else {
			DynamicCharBuffer dynamicBuffer = new DynamicCharBuffer();
			size = (int) copyChars(container, dynamicBuffer);
			container = dynamicBuffer;
		}
		char [] chars = new char[size];
		StaticCharBuffer buffer = new StaticCharBuffer(chars, false);
		// this should only happen if you are multithreading
		if (copyChars(container, buffer) != chars.length)
			throw new IOException("Could not copy all the data");
		return chars;
	}
	
	/**
	 * It has to be limited in size because by definition it can't be done in increments (like a read)
	 * The increments require the internal state of the object to be updated which isn't done when peeking
	 * @throws IOException 
	 */
	public static <T extends ReadableContainer<ByteBuffer> & PeekableContainer<ByteBuffer> & LimitedReadableContainer<ByteBuffer>> byte [] peekBytes(T container) throws IOException {
		int size = (int) container.remainingData();
		byte [] bytes = new byte[size];
		StaticByteBuffer resultBuffer = new StaticByteBuffer(bytes, false);
		ByteBuffer buffer = new CyclicByteBuffer(4096);
		long read = 0;
		while ((read = container.read(buffer)) > 0) {
			long written = resultBuffer.write(buffer);
			if (written != read)
				throw new IOException("The output container is not big enough to copy the input to");
		}
		return bytes;
	}

	public static <T extends ReadableContainer<CharBuffer> & PeekableContainer<CharBuffer> & LimitedReadableContainer<CharBuffer>> char [] peekChars(T container) throws IOException {
		int size = (int) container.remainingData();
		char [] chars = new char[size];
		StaticCharBuffer resultBuffer = new StaticCharBuffer(chars, false);
		CharBuffer buffer = new CyclicCharBuffer(4096);
		long read = 0;
		while ((read = container.read(buffer)) > 0) {
			long written = resultBuffer.write(buffer);
			if (written != read)
				throw new IOException("The output container is not big enough to copy the input to");
		}
		return chars;
	}

	
	public static long copyBytes(ReadableContainer<ByteBuffer> input, WritableContainer<ByteBuffer> output) throws IOException {
		return copy(input, output, new CyclicByteBuffer(4096));
	}
	
	public static long copyChars(ReadableContainer<CharBuffer> input, WritableContainer<CharBuffer> output) throws IOException {
		return copy(input, output, new CyclicCharBuffer(4096));
	}
	
	public static <T extends Buffer<T>> long copy(ReadableContainer<T> input, WritableContainer<T> output, T buffer) throws IOException {
		long read = 0;
		long totalCopied = 0;
		// if the output is limited and we know the limit, only read as much as you can write from the input
		// otherwise we are forced to throw an exception when the output is not big enough
		LimitedWritableContainer<T> limitedOutput = output instanceof LimitedWritableContainer ? (LimitedWritableContainer<T>) output : null;
		// resize buffer
		if (limitedOutput != null) {
			if (limitedOutput.remainingSpace() == 0)
				return 0;
			else if (limitedOutput.remainingSpace() < buffer.remainingSpace())
				buffer = buffer.getFactory().newInstance(limitedOutput.remainingSpace(), true);
		}
		while ((read = input.read(buffer)) > 0) {
			long written = output.write(buffer);
			if (written != read)
				throw new IOException("The output container of type " + output.getClass().getName() + " is not big enough to copy the input to");
			totalCopied += read;
			// resize buffer
			if (limitedOutput != null) {
				if (limitedOutput.remainingSpace() == 0)
					break;
				else if (limitedOutput.remainingSpace() < buffer.remainingSpace())
					buffer = buffer.getFactory().newInstance(limitedOutput.remainingSpace(), true);
			}
		}
		return totalCopied;
	}
	
	public static ByteBuffer newByteBuffer() {
		return new DynamicByteBuffer();
	}
	
	public static ByteBuffer newByteSink() {
		return new ByteBufferSink(-1);
	}
	public static ByteBuffer newByteSink(long amount) {
		return new ByteBufferSink(amount);
	}
	
	public static CharBuffer newCharBuffer() {
		return new DynamicCharBuffer();
	}

	public static CharBuffer newCharSink(long amount) {
		return new CharBufferSink(amount);
	}
	public static CharBuffer newCharSink() {
		return new CharBufferSink(-1);
	}
	
	public static CharBuffer newCharBuffer(int size, boolean cyclic) {
		return cyclic ? new CyclicCharBuffer(size) : new StaticCharBuffer(size);
	}
	public static ByteBuffer newByteBuffer(int size, boolean cyclic) {
		return cyclic ? new CyclicByteBuffer(size) : new StaticByteBuffer(size);
	}
	
	public static Container<ByteBuffer> wrap(File file) {
		return new FileWrapper(file);
	}
	
	public static ReadableContainer<CharBuffer> wrap(Reader reader) {
		return new ReaderWrapper(reader);
	}
	public static WritableContainer<CharBuffer> wrap(Writer writer) {
		return new WriterWrapper(writer);
	}
	
	public static ByteBuffer wrap(byte [] bytes, int offset, int length, boolean containsData) {
		return new StaticByteBuffer(bytes, offset, length, containsData);
	}
	
	public static ByteBuffer wrap(byte [] bytes, boolean containsData) {
		return new StaticByteBuffer(bytes, containsData);
	}
	
	public static CharBuffer wrap(char [] chars, int offset, int length, boolean containsData) {
		return new StaticCharBuffer(chars, offset, length, containsData);
	}
	
	public static CharBuffer wrap(char [] bytes, boolean containsData) {
		return new StaticCharBuffer(bytes, containsData);
	}
	
	public static StaticCharBuffer wrap(String text) {
		return new StaticCharBuffer(text.toCharArray(), true);
	}
	
	public static <T extends Buffer<T>> ReadableContainer<T> bufferReadable(ReadableContainer<T> input, T buffer) {
		return new BufferedReadableContainer<T>(input, buffer);
	}
	public static <T extends Buffer<T>> WritableContainer<T> bufferWritable(WritableContainer<T> output, T buffer) {
		return new BufferedWritableContainer<T>(output, buffer);
	}
	
	public static <T extends Buffer<T>> Container<T> wrap(ReadableContainer<T> readable, WritableContainer<T> writable) {
		return new ComposedContainer<T>(readable, writable);
	}
	
	public static ReadableContainer<CharBuffer> wrapReadable(ReadableContainer<ByteBuffer> bytes, Charset charset) {
		return new BackedReadableCharContainer(bytes, charset);
	}
	public static WritableContainer<CharBuffer> wrapWritable(WritableContainer<ByteBuffer> bytes, Charset charset) {
		return new BackedWritableCharContainer(bytes, charset);
	}

	public static Container<CharBuffer> wrap(Container<ByteBuffer> bytes, Charset charset) {
		return new ComposedContainer<CharBuffer>(
			wrapReadable(bytes, charset),
			wrapWritable(bytes, charset)
		);
	}
	
	public static DelimitedCharContainer delimit(ReadableContainer<CharBuffer> parent, String delimiter) {
		return new DelimitedCharContainerImpl(parent, delimiter);
	}
	public static DelimitedCharContainer delimit(ReadableContainer<CharBuffer> parent, String regex, int bufferSize) {
		return new DelimitedCharContainerImpl(parent, regex, bufferSize);
	}
	
	public static <T extends Buffer<T>> MarkableContainer<T> mark(ReadableContainer<T> parent) {
		return mark(parent, 0);
	}
	public static <T extends Buffer<T>> MarkableContainer<T> mark(ReadableContainer<T> parent, long limit) {
		return new LimitedMarkableContainer<T>(parent, limit);
	}
	
	public static <T extends Buffer<T>> PushbackContainer<T> pushback(ReadableContainer<T> parent) {
		if (parent instanceof PushbackContainer)
			return (PushbackContainer<T>) parent;
		else
			return new PushbackContainerImpl<T>(parent);
	}
	
	public static <T extends Buffer<T>> LimitedReadableContainer<T> limitReadable(ReadableContainer<T> parent, long limit) {
		return new LimitedReadableContainerImpl<T>(parent, limit);
	}
	
	public static <T extends Buffer<T>> LimitedWritableContainer<T> limitWritable(WritableContainer<T> parent, long limit) {
		return new LimitedWritableContainerImpl<T>(parent, limit);
	}
	
	public static <T extends Closeable> void close(T...closeables) throws IOException {
		close(true, closeables);
	}
	public static <T extends Closeable> void close(boolean suppressExceptions, T...closeables) throws IOException {
		IOException exception = null;
		for (Closeable closeable : closeables) {
			try {
				closeable.close();
			}
			catch (Exception e) {
				if (exception == null)
					exception = new IOException("Could not close all the closeables", e);
			}
		}
		if (exception != null) {
			if (suppressExceptions)
				exception.printStackTrace(System.err);
			else
				throw exception;
		}
	}
	
	public static <T extends Buffer<T>> ReadableContainer<T> blockUntilRead(ReadableContainer<T> parent) {
		return blockUntilRead(parent,  1);
	}
	public static <T extends Buffer<T>> ReadableContainer<T> blockUntilRead(ReadableContainer<T> parent, long amount) {
		return new MinimalReadableContainer<T>(parent, amount);
	}
	
	public static <T extends Buffer<T>> WritableContainer<T> blockUntilWritten(WritableContainer<T> parent) {
		return blockUntilWritten(parent,  1);
	}
	public static <T extends Buffer<T>> WritableContainer<T> blockUntilWritten(WritableContainer<T> parent, long amount) {
		return new MinimalWritableContainer<T>(parent, amount);
	}

	public static ReadableContainer<ByteBuffer> wrap(InputStream input) {
		return new InputStreamWrapper(input);
	}
	public static WritableContainer<ByteBuffer> wrap(OutputStream output) {
		return new OutputStreamWrapper(output);
	}
	
	public static ReadableContainer<ByteBuffer> unwrap(ReadableContainer<CharBuffer> parent, Charset charset) {
		return new ReadableCharToByteContainer(parent, charset);
	}
	
	public static ByteBuffer wrap(java.nio.ByteBuffer buffer, boolean containsData) {
		return new NioByteBufferWrapper(buffer, containsData);
	}
	public static CharBuffer wrap(java.nio.CharBuffer buffer, boolean containsData) {
		return new NioCharBufferWrapper(buffer, containsData);
	}
	public static <T extends ByteChannel> Container<ByteBuffer> wrap(T channel) {
		return new ByteChannelContainer<ByteChannel>(channel);
	}
	
	public static Container<ByteBuffer> secure(Container<ByteBuffer> container, SSLContext context, boolean isClient) throws SSLException {
		return new SSLSocketByteContainer(container, context, isClient);
	}
	
	public static Container<ByteBuffer> connect(String host, int port, SocketOption<?>...options) throws IOException {
		return new SocketByteContainer(new InetSocketAddress(host, port));
	}

	public static ReadableContainer<CharBuffer> validate(ReadableContainer<CharBuffer> container, char [] characters, boolean whitelist) {
		return new ValidatedReadableCharContainer(container, characters, whitelist);
	}

	public static <T extends Buffer<T>> CountingReadableContainer<T> countReadable(ReadableContainer<T> container) {
		return new CountingReadableContainerImpl<T>(container);
	}
	public static <T extends Buffer<T>> CountingWritableContainer<T> countWritable(WritableContainer<T> container) {
		return new CountingWritableContainerImpl<T>(container);
	}
	
	public static <T extends Buffer<T>> ReadableContainer<T> synchronizeReadable(ReadableContainer<T> container) {
		return new SynchronizedReadableContainer<T>(container, new ReentrantLock());
	}
	
	public static <T extends Buffer<T>> WritableContainer<T> synchronizeWritable(WritableContainer<T> container) {
		return new SynchronizedWritableContainer<T>(container, new ReentrantLock());
	}
	public static <T extends Buffer<T>> Container<T> synchronize(Container<T> container) {
		Lock lock = new ReentrantLock();
		return wrap(new SynchronizedReadableContainer<T>(container, lock), new SynchronizedWritableContainer<T>(container, lock));
	}
	
	public static <T extends Buffer<T>> WritableContainer<T> multicast(List<? extends WritableContainer<T>> outputs) {
		return new WritableContainerMulticaster<T>(outputs);
	}
	
	public static <T extends Buffer<T>> WritableContainer<T> multicast(WritableContainer<T>...outputs) {
		return new WritableContainerMulticaster<T>(outputs);
	}
	public static <T extends Buffer<T>> ReadableContainer<T> chain(ReadableContainer<T>...inputs) {
		return new ReadableContainerChainer<T>(inputs);
	}

	public static Container<ByteBuffer> digest(WritableContainer<ByteBuffer> chainedOutput, MessageDigest digest) {
		return new ByteContainerDigest(chainedOutput, digest);
	}
	
	public static <T extends Buffer<T>> ReadableContainer<T> fixReadableLength(ReadableContainer<T> container, long fixedLength) {
		return new FixedLengthReadableContainer<T>(container, fixedLength);
	}

	public static InputStream toInputStream(ReadableContainer<ByteBuffer> readable) {
		return new ByteContainerInputStream(readable);
	}
	
	public static OutputStream toOutputStream(WritableContainer<ByteBuffer> writable) {
		return new ByteContainerOutputStream(writable);
	}
	
	public static Reader toReader(ReadableContainer<CharBuffer> readable) {
		return new CharContainerReader(readable);
	}
	public static Writer toWriter(WritableContainer<CharBuffer> writable) {
		return new CharContainerWriter(writable);
	}
}
