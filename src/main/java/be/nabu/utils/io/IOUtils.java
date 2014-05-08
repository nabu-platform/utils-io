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
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ByteChannel;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.net.ssl.SSLContext;

import be.nabu.utils.io.api.ByteContainer;
import be.nabu.utils.io.api.CharContainer;
import be.nabu.utils.io.api.CountingReadableByteContainer;
import be.nabu.utils.io.api.CountingReadableCharContainer;
import be.nabu.utils.io.api.CountingWritableByteContainer;
import be.nabu.utils.io.api.CountingWritableCharContainer;
import be.nabu.utils.io.api.DelimitedCharContainer;
import be.nabu.utils.io.api.IORuntimeException;
import be.nabu.utils.io.api.LimitedReadableByteContainer;
import be.nabu.utils.io.api.LimitedReadableCharContainer;
import be.nabu.utils.io.api.LimitedWritableByteContainer;
import be.nabu.utils.io.api.LimitedWritableCharContainer;
import be.nabu.utils.io.api.MarkableByteContainer;
import be.nabu.utils.io.api.MarkableCharContainer;
import be.nabu.utils.io.api.PushbackByteContainer;
import be.nabu.utils.io.api.PushbackCharContainer;
import be.nabu.utils.io.api.ReadableByteContainer;
import be.nabu.utils.io.api.ReadableCharContainer;
import be.nabu.utils.io.api.ResettableByteContainer;
import be.nabu.utils.io.api.ResettableCharContainer;
import be.nabu.utils.io.api.SkippableByteContainer;
import be.nabu.utils.io.api.SkippableCharContainer;
import be.nabu.utils.io.api.WritableByteContainer;
import be.nabu.utils.io.api.WritableCharContainer;
import be.nabu.utils.io.impl.BackedReadableCharContainer;
import be.nabu.utils.io.impl.BackedWritableCharContainer;
import be.nabu.utils.io.impl.BufferedReadableByteContainer;
import be.nabu.utils.io.impl.BufferedReadableCharContainer;
import be.nabu.utils.io.impl.BufferedWritableByteContainer;
import be.nabu.utils.io.impl.BufferedWritableCharContainer;
import be.nabu.utils.io.impl.ByteBufferWrapper;
import be.nabu.utils.io.impl.ByteChannelContainer;
import be.nabu.utils.io.impl.ByteContainerDigest;
import be.nabu.utils.io.impl.ByteContainerSink;
import be.nabu.utils.io.impl.CharBufferWrapper;
import be.nabu.utils.io.impl.CharContainerSink;
import be.nabu.utils.io.impl.ComposedByteContainer;
import be.nabu.utils.io.impl.ComposedCharContainer;
import be.nabu.utils.io.impl.CountingReadableByteContainerImpl;
import be.nabu.utils.io.impl.CountingReadableCharContainerImpl;
import be.nabu.utils.io.impl.CountingWritableByteContainerImpl;
import be.nabu.utils.io.impl.CountingWritableCharContainerImpl;
import be.nabu.utils.io.impl.CyclicByteContainer;
import be.nabu.utils.io.impl.CyclicCharContainer;
import be.nabu.utils.io.impl.DelimitedCharContainerImpl;
import be.nabu.utils.io.impl.DynamicByteContainer;
import be.nabu.utils.io.impl.DynamicCharContainer;
import be.nabu.utils.io.impl.FileWrapper;
import be.nabu.utils.io.impl.FixedLengthReadableByteContainer;
import be.nabu.utils.io.impl.InputStreamWrapper;
import be.nabu.utils.io.impl.LimitedMarkableByteContainer;
import be.nabu.utils.io.impl.LimitedMarkableCharContainer;
import be.nabu.utils.io.impl.LimitedReadableByteContainerImpl;
import be.nabu.utils.io.impl.LimitedReadableCharContainerImpl;
import be.nabu.utils.io.impl.LimitedWritableByteContainerImpl;
import be.nabu.utils.io.impl.LimitedWritableCharContainerImpl;
import be.nabu.utils.io.impl.MinimalReadableByteContainer;
import be.nabu.utils.io.impl.MinimalWritableByteContainer;
import be.nabu.utils.io.impl.OutputStreamWrapper;
import be.nabu.utils.io.impl.PushbackByteContainerImpl;
import be.nabu.utils.io.impl.PushbackCharContainerImpl;
import be.nabu.utils.io.impl.ReadableByteContainerChainer;
import be.nabu.utils.io.impl.ReadableByteContainerInputStream;
import be.nabu.utils.io.impl.ReadableCharContainerChainer;
import be.nabu.utils.io.impl.ReadableCharContainerReader;
import be.nabu.utils.io.impl.ReadableCharToByteContainer;
import be.nabu.utils.io.impl.ReaderWrapper;
import be.nabu.utils.io.impl.SSLSocketByteContainer;
import be.nabu.utils.io.impl.SocketByteContainer;
import be.nabu.utils.io.impl.SynchronizedReadableByteContainer;
import be.nabu.utils.io.impl.SynchronizedReadableCharContainer;
import be.nabu.utils.io.impl.SynchronizedWritableByteContainer;
import be.nabu.utils.io.impl.SynchronizedWritableCharContainer;
import be.nabu.utils.io.impl.ValidatedReadableCharContainer;
import be.nabu.utils.io.impl.WritableByteContainerCombiner;
import be.nabu.utils.io.impl.WritableByteContainerOutputStream;
import be.nabu.utils.io.impl.WritableCharContainerCombiner;
import be.nabu.utils.io.impl.WritableCharContainerWriter;
import be.nabu.utils.io.impl.WriterWrapper;

public class IOUtils {

	public static final int OPTIMAL_BUFFERSIZE = 102400;
	
	public static ReadableCharContainer validate(ReadableCharContainer container, char [] characters, boolean whitelist) {
		return new ValidatedReadableCharContainer(container, characters, whitelist);
	}
	
	public static CharContainer newCharBuffer(int size) {
		return new CyclicCharContainer(size);
	}

	public static ByteContainer newByteBuffer(int size) {
		return new CyclicByteContainer(size);
	}
	
	public static DelimitedCharContainer delimit(ReadableCharContainer parent, String delimiter) {
		return new DelimitedCharContainerImpl(parent, delimiter);
	}
	public static DelimitedCharContainer delimit(ReadableCharContainer parent, String regex, int bufferSize) {
		return new DelimitedCharContainerImpl(parent, regex, bufferSize);
	}
	
	public static long skip(ReadableByteContainer container, long amount) {
		if (container instanceof SkippableByteContainer)
			return ((SkippableByteContainer) container).skip(amount);
		else
			return copy(container, newByteSink(), amount);
	}
	
	public static long skip(ReadableCharContainer container, long amount) {
		if (container instanceof SkippableCharContainer)
			return ((SkippableCharContainer) container).skip(amount);
		else
			return copy(container, newCharSink(), amount);
	}
	
	public static ReadableByteContainer bufferInput(ReadableByteContainer container) {
		return bufferInput(container, OPTIMAL_BUFFERSIZE);
	}
	public static ReadableByteContainer bufferInput(ReadableByteContainer container, int bufferSize) {
		return new BufferedReadableByteContainer(container, bufferSize);
	}
	public static WritableByteContainer bufferOutput(WritableByteContainer container) {
		return bufferOutput(container, OPTIMAL_BUFFERSIZE);
	}
	public static WritableByteContainer bufferOutput(WritableByteContainer container, int bufferSize) {
		return new BufferedWritableByteContainer(container, bufferSize);
	}
	public static ReadableCharContainer bufferInput(ReadableCharContainer container) {
		return bufferInput(container, OPTIMAL_BUFFERSIZE);
	}
	public static ReadableCharContainer bufferInput(ReadableCharContainer container, int bufferSize) {
		return new BufferedReadableCharContainer(container, bufferSize);
	}
	public static WritableCharContainer bufferOutput(WritableCharContainer container) {
		return bufferOutput(container, OPTIMAL_BUFFERSIZE);
	}
	public static WritableCharContainer bufferOutput(WritableCharContainer container, int bufferSize) {
		return new BufferedWritableCharContainer(container, bufferSize);
	}

	public static CountingReadableByteContainer countInput(ReadableByteContainer container) {
		return countInput(container, true);
	}
	
	public static CountingReadableByteContainer countInput(ReadableByteContainer container, boolean forceNew) {
		if (!forceNew && container instanceof CountingReadableByteContainer)
			return (CountingReadableByteContainer) container;
		else
			return new CountingReadableByteContainerImpl(container);
	}
	
	public static CountingWritableByteContainer countOutput(WritableByteContainer container) {
		return countOutput(container, true);
	}
	
	public static CountingWritableByteContainer countOutput(WritableByteContainer container, boolean forceNew) {
		if (!forceNew && container instanceof CountingReadableByteContainer)
			return (CountingWritableByteContainer) container;
		else
			return new CountingWritableByteContainerImpl(container);
	}
	
	public static CountingReadableCharContainer countInput(ReadableCharContainer container) {
		return countInput(container, true);
	}
	
	public static CountingReadableCharContainer countInput(ReadableCharContainer container, boolean forceNew) {
		if (!forceNew && container instanceof CountingReadableCharContainer)
			return (CountingReadableCharContainer) container;
		else
			return new CountingReadableCharContainerImpl(container);
	}
	
	public static CountingWritableCharContainer countOutput(WritableCharContainer container) {
		return countOutput(container, true);
	}
	
	public static CountingWritableCharContainer countOutput(WritableCharContainer container, boolean forceNew) {
		if (!forceNew && container instanceof CountingReadableCharContainer)
			return (CountingWritableCharContainer) container;
		else
			return new CountingWritableCharContainerImpl(container);
	}
	
	public static ReadableCharContainer synchronize(ReadableCharContainer container) {
		return new SynchronizedReadableCharContainer(container, new ReentrantLock());
	}
	
	public static WritableCharContainer synchronize(WritableCharContainer container) {
		return new SynchronizedWritableCharContainer(container, new ReentrantLock());
	}
	
	public static CharContainer synchronize(CharContainer container) {
		Lock lock = new ReentrantLock();
		return new ComposedCharContainer(new SynchronizedReadableCharContainer(container, lock), new SynchronizedWritableCharContainer(container, lock));
	}
	
	public static WritableByteContainer combine(WritableByteContainer...outputs) {
		return new WritableByteContainerCombiner(outputs);
	}
	public static WritableCharContainer combine(WritableCharContainer...outputs) {
		return new WritableCharContainerCombiner(outputs);
	}
	
	public static ReadableByteContainer chain(ReadableByteContainer...inputs) {
		return new ReadableByteContainerChainer(inputs);
	}
	
	public static ReadableCharContainer chain(ReadableCharContainer...inputs) {
		return new ReadableCharContainerChainer(inputs);
	}
	
	public static ByteContainer digest(MessageDigest digest, WritableByteContainer chainedOutput) {
		return new ByteContainerDigest(digest, chainedOutput);
	}
	
	public static ReadableByteContainer synchronize(ReadableByteContainer container) {
		return new SynchronizedReadableByteContainer(container, new ReentrantLock());
	}
	
	public static WritableByteContainer synchronize(WritableByteContainer container) {
		return new SynchronizedWritableByteContainer(container, new ReentrantLock());
	}
	
	public static ByteContainer synchronize(ByteContainer container) {
		Lock lock = new ReentrantLock();
		return new ComposedByteContainer(new SynchronizedReadableByteContainer(container, lock), new SynchronizedWritableByteContainer(container, lock));
	}
	
	public static ReadableByteContainer wrapFixedLength(ReadableByteContainer container, long fixedLength) {
		return new FixedLengthReadableByteContainer(container, fixedLength);
	}
	
	public static ResettableByteContainer wrapResettable(ReadableByteContainer readable) {
		if (readable instanceof ResettableByteContainer)
			return (ResettableByteContainer) readable;
		else {
			MarkableByteContainer markable = wrapMarkable(readable);
			markable.mark();
			return markable;
		}
	}
	
	public static ByteContainer wrapSSL(ByteContainer container, SSLContext context, boolean isClient) {
		try {
			return new SSLSocketByteContainer(container, context, isClient);
		}
		catch (IOException e) {
			throw new IORuntimeException(e);
		}
	}

	public static ResettableCharContainer wrapResettable(ReadableCharContainer readable) {
		if (readable instanceof ResettableCharContainer)
			return (ResettableCharContainer) readable;
		else {
			MarkableCharContainer markable = wrapMarkable(readable);
			markable.mark();
			return markable;
		}
	}
	
	public static MarkableByteContainer wrapMarkable(ReadableByteContainer readable) {
		if (readable instanceof MarkableByteContainer)
			return (MarkableByteContainer) readable;
		else
			return new LimitedMarkableByteContainer(readable, 0);
	}
	
	public static MarkableCharContainer wrapMarkable(ReadableCharContainer readable) {
		if (readable instanceof MarkableCharContainer)
			return (MarkableCharContainer) readable;
		else
			return new LimitedMarkableCharContainer(readable, 0);
	}
	
	public static MarkableByteContainer wrapMarkable(ReadableByteContainer readable, long readLimit) {
		return new LimitedMarkableByteContainer(readable, readLimit);
	}
	
	public static MarkableCharContainer wrapMarkable(ReadableCharContainer readable, long readLimit) {
		return new LimitedMarkableCharContainer(readable, readLimit);
	}
	
	public static PushbackByteContainer wrapPushback(ReadableByteContainer readable) {
		if (readable instanceof PushbackByteContainer)
			return (PushbackByteContainer) readable;
		else
			return new PushbackByteContainerImpl(readable);
	}
	
	public static PushbackCharContainer wrapPushback(ReadableCharContainer readable) {
		if (readable instanceof PushbackCharContainer)
			return (PushbackCharContainer) readable;
		else
			return new PushbackCharContainerImpl(readable);
	}
	
	public static ByteContainer wrap(File file) {
		return new FileWrapper(file);
	}
	
	public static ReadableByteContainer wrap(byte [] bytes) {
		ByteContainer container = wrap(bytes, true);
		IOUtils.close(container);
		return container;
	}
	
	public static ReadableCharContainer wrap(char [] chars) {
		CharContainer container = wrap(chars, true);
		IOUtils.close(container);
		return container;
	}
	
	public static ByteContainer wrap(byte [] bytes, int offset, int length, boolean containsData) {
		return new ByteBufferWrapper(ByteBuffer.wrap(bytes, offset, length), containsData);
	}
	
	public static ByteContainer wrap(byte [] bytes, boolean containsData) {
		return new ByteBufferWrapper(ByteBuffer.wrap(bytes), containsData);
	}
	
	public static CharContainer wrap(char [] chars, int offset, int length, boolean containsData) {
		return new CharBufferWrapper(CharBuffer.wrap(chars, offset, length), containsData);
	}
	
	public static CharContainer wrap(char [] chars, boolean containsData) {
		return new CharBufferWrapper(CharBuffer.wrap(chars), containsData);
	}
	
	public static ReadableCharContainer wrap(String content) {
		CharContainer container = wrap(content.toCharArray(), true);
		// to indicate that no new data can be written to it 
		IOUtils.close(container);
		return container;
	}
	
	public static ReadableByteContainer blockUntilRead(ReadableByteContainer parent) {
		return blockUntilRead(parent, 1);
	}
	public static ReadableByteContainer blockUntilRead(ReadableByteContainer parent, int minimumAmountToWrite) {
		return new MinimalReadableByteContainer(parent, minimumAmountToWrite);
	}
	
	public static WritableByteContainer blockUntilWritten(WritableByteContainer parent) {
		return blockUntilWritten(parent, 1);
	}
	public static WritableByteContainer blockUntilWritten(WritableByteContainer parent, int minimumAmountToWrite) {
		return new MinimalWritableByteContainer(parent, minimumAmountToWrite);
	}
	
	public static ByteContainer wrap(ByteBuffer buffer, boolean containsData) {
		return new ByteBufferWrapper(buffer, containsData);
	}
	
	public static CharContainer wrap(CharBuffer buffer, boolean containsData) {
		return new CharBufferWrapper(buffer, containsData);
	}
	
	public static ReadableByteContainer wrap(InputStream input) {
		return new InputStreamWrapper(input);
	}
	
	public static ReadableCharContainer wrap(Reader reader) {
		return new ReaderWrapper(reader);
	}
	
	public static WritableCharContainer wrap(Writer writer) {
		return new WriterWrapper(writer);
	}
	
	public static WritableByteContainer wrap(OutputStream output) {
		return new OutputStreamWrapper(output);
	}
	
	public static ReadableByteContainer unwrap(ReadableCharContainer parent, Charset charset) {
		return new ReadableCharToByteContainer(parent, charset);
	}
	
	public static ReadableCharContainer wrap(ReadableByteContainer byteContainer, Charset charset) {
		return new BackedReadableCharContainer(byteContainer, charset);
	}
	
	public static WritableCharContainer wrap(WritableByteContainer byteContainer, Charset charset) {
		return new BackedWritableCharContainer(byteContainer, charset);
	}
	
	public static CharContainer wrap(ByteContainer byteContainer, Charset charset) {
		return wrap(new BackedReadableCharContainer(byteContainer, charset), new BackedWritableCharContainer(byteContainer, charset));
	}
	
	public static ByteContainer wrap(ReadableByteContainer readable, WritableByteContainer writable) {
		return new ComposedByteContainer(readable, writable);
	}
	
	public static CharContainer wrap(ReadableCharContainer readable, WritableCharContainer writable) {
		return new ComposedCharContainer(readable, writable);
	}
	
	public static InputStream toInputStream(ReadableByteContainer container) {
		return new ReadableByteContainerInputStream(container);
	}
	
	public static OutputStream toOutputStream(WritableByteContainer container) {
		return new WritableByteContainerOutputStream(container);
	}
	
	public static Reader toReader(ReadableCharContainer container) {
		return new ReadableCharContainerReader(container);
	}
	
	public static Writer toWriter(WritableCharContainer container) {
		return new WritableCharContainerWriter(container);
	}
	
	public static <T extends ByteChannel> ByteContainer wrap(T channel) {
		return new ByteChannelContainer<T>(channel);
	}
	
	public static ByteContainer connect(String host, int port, SocketOption<?>...options) {
		try {
			return new SocketByteContainer(new InetSocketAddress(host, port));
		}
		catch (IOException e) {
			throw new IORuntimeException(e);
		}
	}
	
	public static ByteContainer newByteContainer() {
		return new DynamicByteContainer();
	}
	
	public static CharContainer newCharContainer() {
		return new DynamicCharContainer();
	}
	
	public static WritableByteContainer newByteSink() {
		return new ByteContainerSink();
	}
	
	public static WritableCharContainer newCharSink() {
		return new CharContainerSink();
	}
	
	public static LimitedReadableByteContainer limitReadable(ReadableByteContainer container, long limit) {
		return new LimitedReadableByteContainerImpl(container, limit);
	}
	
	public static LimitedWritableByteContainer limitWritable(WritableByteContainer container, long limit) {
		return new LimitedWritableByteContainerImpl(container, limit);
	}
	
	public static LimitedReadableCharContainer limitReadable(ReadableCharContainer container, long limit) {
		return new LimitedReadableCharContainerImpl(container, limit);
	}
	
	public static LimitedWritableCharContainer limitWritable(WritableCharContainer container, long limit) {
		return new LimitedWritableCharContainerImpl(container, limit);
	}
	
	public static long copy(ReadableByteContainer input, WritableByteContainer output) {
		long maxAmount = Long.MAX_VALUE;
		if (output instanceof LimitedWritableByteContainer)
			maxAmount = ((LimitedWritableByteContainer) output).remainingSpace();
		return copy(input, output, maxAmount, 0, TimeUnit.MILLISECONDS, false);
	}
	
	public static long copy(ReadableByteContainer input, WritableByteContainer output, long maxAmount) {
		return copy(input, output, maxAmount, 0, TimeUnit.MILLISECONDS, false);
	}
	
	public static long copy(ReadableByteContainer input, WritableByteContainer output, long timeout, TimeUnit timeUnit) {
		return copy(input, output, Long.MAX_VALUE, timeout, timeUnit, false);
	}
	
	public static long copy(ReadableByteContainer input, WritableByteContainer output, long timeout, TimeUnit timeUnit, boolean tryFull) {
		return copy(input, output, Long.MAX_VALUE, timeout, timeUnit, tryFull);
	}
	
	/**
	 * Copies the input to the output. It reads until there is no more data (either nothing is read or the input is closed)
	 * Inputstreams are blocking until data is returned so they should never return 0.
	 * Bytebuffers etc are non-blocking so the user takes control of when to read or write.
	 * Note that this keeps trying to copy the data to the output for as long as the timeout is set
	 * Also note that if tryFull is done, it will also use the timeout for the input
	 * 
	 * If the timeout for the write is exceeded, the method will throw an exception (an unknown number of bytes will have been read from the input but not written to the output)
	 * If the timeout for the read is exceeded, no data was left in limbo so the code just stops
	 */
	public static long copy(ReadableByteContainer input, WritableByteContainer output, long maxAmount, long timeout, TimeUnit timeUnit, boolean tryFull) {
		if (timeout != 0)
			timeout = timeUnit.convert(timeout, TimeUnit.MILLISECONDS);
		int read = 0;
		int remaining = 0;
		byte [] bytes = new byte[102400];
		long totalWritten = 0;
		Date lastWritten = timeout == 0 ? null : new Date();
		Date lastRead = timeout == 0 ? null : new Date();
		while (maxAmount > 0 && (remaining > 0 || (remaining = read = input.read(bytes, 0, (int) Math.min(maxAmount, bytes.length))) > (tryFull ? -1 : 0))) {
			if (read == 0) {
				// if there is no timeout, retry indefinitely
				if (timeout == 0)
					continue;
				// if we have exceeded the timeout, stop reading
				else if (new Date().getTime() - lastRead.getTime() > timeout)
					break;
			}
			else if (timeout > 0)
				lastRead = new Date();
			int written = output.write(bytes, read - remaining, remaining);
			totalWritten += written;
			maxAmount -= written;
			remaining -= written;
			if (remaining > 0) {
				// no timeout set, just fail
				if (timeout == 0)
					throw new IORuntimeException("The target container was not large enough to copy the entire input. The write was not retried as the timeout is set to 0");
				// timed out, fail
				else if (new Date().getTime() - lastWritten.getTime() >= timeout)
					throw new IORuntimeException("The target container was not large enough to copy the entire input. The write was retried for " + timeout + " milliseconds");
			}
			else if (timeout > 0)
				lastWritten = new Date();
		}
		return totalWritten;
	}
	
	public static long copy(ReadableCharContainer input, WritableCharContainer output) {
		long maxAmount = Long.MAX_VALUE;
		if (output instanceof LimitedWritableCharContainer)
			maxAmount = ((LimitedWritableCharContainer) output).remainingSpace();
		return copy(input, output, maxAmount, 0, TimeUnit.MILLISECONDS, false);
	}
	
	public static long copy(ReadableCharContainer input, WritableCharContainer output, long maxAmount) {
		return copy(input, output, maxAmount, 0, TimeUnit.MILLISECONDS, false);
	}
	
	public static long copy(ReadableCharContainer input, WritableCharContainer output, long timeout, TimeUnit timeUnit) {
		return copy(input, output, timeout, timeUnit, false);
	}
	public static long copy(ReadableCharContainer input, WritableCharContainer output, long timeout, TimeUnit timeUnit, boolean tryFull) {
		return copy(input, output, Long.MAX_VALUE, timeout, timeUnit, tryFull);
	}
	
	public static long copy(ReadableCharContainer input, WritableCharContainer output, long maxAmount, long timeout, TimeUnit timeUnit, boolean tryFull) {
		if (timeout != 0)
			timeout = timeUnit.convert(timeout, TimeUnit.MILLISECONDS);
		int read = 0;
		int remaining = 0;
		char [] chars = new char[102400];
		long totalWritten = 0;
		Date lastWritten = timeout == 0 ? null : new Date();
		Date lastRead = timeout == 0 ? null : new Date();
		while (maxAmount > 0 && (remaining > 0 || (remaining = read = input.read(chars, 0, (int) Math.min(maxAmount, chars.length))) > (tryFull ? -1 : 0))) {
			if (read == 0) {
				// if there is no timeout, retry indefinitely
				if (timeout == 0)
					continue;
				// if we have exceeded the timeout, stop reading
				else if (new Date().getTime() - lastRead.getTime() > timeout)
					break;
			}
			else if (timeout > 0)
				lastRead = new Date();
			int written = output.write(chars, read - remaining, remaining);
			totalWritten += written;
			maxAmount -= written;
			remaining -= written;
			if (remaining > 0) {
				// no timeout set, just fail
				if (timeout == 0)
					throw new IORuntimeException("The target container was not large enough to copy the entire input. The write was not retried as the timeout is set to 0");
				// timed out, fail
				else if (new Date().getTime() - lastWritten.getTime() >= timeout)
					throw new IORuntimeException("The target container was not large enough to copy the entire input. The write was retried for " + timeout + " milliseconds");
			}
			else if (timeout > 0)
				lastWritten = new Date();
		}
		return totalWritten;
	}
	
	public static String toString(ReadableCharContainer input) {
		return toString(input, false);
	}
	
	public static String toString(ReadableCharContainer input, boolean forceFull) {
		StringBuilder builder = new StringBuilder();
		int read = 0;
		char [] chars = new char[102400];
		boolean hasRead = false;
		while ((read = input.read(chars)) > (forceFull ? -1 : 0)) {
			hasRead = true;
			builder.append(new String(chars, 0, read));
		}
		String result = builder.toString();
		return !hasRead && read == -1 ? null : result;
	}
	
	public static byte [] toBytes(ReadableByteContainer input) {
		return toBytes(input, 0, null);
	}
	
	public static byte [] toBytes(ReadableByteContainer input, long timeout, TimeUnit timeUnit) {
		// we need to know how large the resulting byte array is
		if (!(input instanceof LimitedReadableByteContainer)) {
			DynamicByteContainer dynamicByteContainer = new DynamicByteContainer();
			copy(input, dynamicByteContainer);
			input = dynamicByteContainer;
		}
		byte [] result = new byte[(int) ((LimitedReadableByteContainer) input).remainingData()];
		ByteContainer container = wrap(result, false);
		if (copy(input, container, timeout, timeUnit) != result.length)
			throw new IORuntimeException("Could not copy all the bytes");
		return result;
	}
	
	public static <T extends Closeable> void close(T...closeables) {
		close(true, closeables);
	}
	
	public static <T extends Closeable> void close(boolean suppressExceptions, T...closeables) {
		IORuntimeException exception = null;
		for (Closeable closeable : closeables) {
			try {
				closeable.close();
			}
			catch (Exception e) {
				if (exception == null)
					exception = new IORuntimeException("Could not close all the closeables", e);
				else
					exception.addSuppressedException(exception);
			}
		}
		if (exception != null) {
			if (suppressExceptions)
				exception.printStackTrace(System.err);
			else
				throw exception;
		}
	}
}
