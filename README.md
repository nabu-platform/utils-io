# Description

The are a couple of problems with I/O in java, at the core is that there are two fundamentally different ways of doing I/O but no general way to support both.
Partly because there is no overlap in the design and partly because both implementations use abstract base classes.

On top of that the whole "java.nio.Buffer" design has some caveats. 
ByteBuffer -to take a specific implementation- requires you to use flip() and compact() correctly in order to manipulate the internal state,
this is both needlessly complex and actually very opaque when you pass a ByteBuffer around. 
If you have a ByteBuffer as input parameter of your method, how do you know whether you have to flip() it first or if the caller has done this?
What does ReadableByteChannel expect? A ByteBuffer that has been compact()-ed I presume but this is not enforceable and sometimes badly documented.

Next let's take Channel, it is a lightweight interface around Closeable and offers little of value, the actually interesting interfaces are:

- ReadableByteChannel
- WritableByteChannel

Note however that while Buffer has distinct implementations for ByteBuffer, CharBuffer, etc, there is only one type of channel: a ByteChannel.
Where is the Writer/Reader equivalent for Channel? Why have 7 types of Buffer but only one type of Channel?
Why are buffers and channels completely separate concepts although they both, fundamentally, provide read/write access to data?

There is a reason that there is no generic "ReadableChannel" and only the specific "ReadableByteChannel": 
because java.nio.Buffer does not allow generic read/write, only the specific implementations (ByteBuffer et al) offer this.
This means you can not do "generic" operations on a buffer which in turn limits the potential scope of a readable channel to a specific type of buffer.

This I/O library has focused on providing a new way of doing I/O that abstracts over async and sync I/O to allow you to write utilities that can manage both.
Additionally it offers a cleaner design of buffers and channels though the latter are called containers.
Interestingly enough, the Buffers are actually also containers, just with a lot more features.

The design is based primarily on java.io, java.nio and netty io.

**Important**: the testcases might fail with maven 2 if you are not running your system in UTF-8 mode. Maven 2 appears to ignore the encoding in the pom.xml file and some testcases are sensitive to encoding. Tests do pass in maven 3.

# Example

The first question with any new library is: how easy is it to use?

## Simple Example

This simple example was pulled from the unit test for FileWrapper:

```java
File target = File.createTempFile("test", ".txt");
String testString = "writing some sp�cial ch�ract�rs";
	
// creates a container around the file, if the file does not exist, it is not created until you write to it
Container<ByteBuffer> bytes = IOUtils.wrap(target);
	
// creates a char container around it so we can work with strings and the like, encoding is UTF-8
Container<CharBuffer> chars = IOUtils.wrap(bytes, Charset.forName("UTF-8"));
	
// write a string, the String is wrapped in a CharBuffer by calling IOUtils.wrap()
chars.write(IOUtils.wrap(testString));
	
// make sure it is flushed to the file system
chars.flush();
	
assertEquals(testString, IOUtils.toString(chars));
assertTrue(target.exists());
assertEquals(34, target.length());
```

Adding buffering (both input & output) to the file container would entail adding this code before wrapping it in a char container:

```java
bytes = IOUtils.wrap(
	IOUtils.bufferReadable(bytes, IOUtils.newByteBuffer(10, true)),
	IOUtils.bufferWritable(bytes, IOUtils.newByteBuffer(10, true))
);
```

## Slightly More Complex Example

This example was pulled from a testcase for quoted printable encoding:

```java
String string = "this is just some data, it doesn't matter what";
// create a new endless memory-based byte buffer:
Container<ByteBuffer> encoded = IOUtils.newByteBuffer();
// we wrap a container around it where the read is untouched but everything that is written to it is encoded with quoted-printable
encoded = IOUtils.wrap(
	encoded,
	TranscoderUtils.wrapWritable(encoded, new QuotedPrintableEncoder(QuotedPrintableEncoding.DEFAULT))
);
// we wrap it again, the read is still undisturbed but everything that is written is now gzipped
encoded = IOUtils.wrap(
	encoded,
	TranscoderUtils.wrapWritable(encoded, new GZIPEncoder())
);
// let's write something, it will first pass the gzip layer and then be encoded with quoted-printable
encoded.write(IOUtils.wrap(string.getBytes(), true));
// flush everything, this will -in this case- write a gzip footer
encoded.flush();

// reading all the data, because the read-part of the container was left undisturbed we can read from it the fully first-gzipped-then-encoded data
byte [] encodedBytes = IOUtils.toBytes(encoded);

assertEquals("=1F=8B=08=00=00=00=00=00=00=00+=C9=C8,V=00=A2=AC=D2=E2=12=85=E2=FC=DCT=85=\r\n"
		+ "=94=C4=92D=1D=85=CC=12=85=94=FC=D4=E2<=F5=12=85=DC=C4=92=92=D4\"=85=F2=8C=C4=\r\n"
		+ "=12=00=828=91=B8.=00=00=00", new String(encodedBytes));

// for decoding you could opt to decode on read or on write
Container<ByteBuffer> decoded = IOUtils.newByteBuffer();
decoded = IOUtils.wrap(
	decoded,
	TranscoderUtils.wrapWritable(decoded, new GZIPDecoder())
);
decoded = IOUtils.wrap(
	decoded,
	TranscoderUtils.wrapWritable(decoded, new QuotedPrintableDecoder(QuotedPrintableEncoding.DEFAULT))
);
// what we write here will first pass through the quoted-printable decoder, then through the gzip decoder
decoded.write(IOUtils.wrap(encodedBytes, true));
decoded.close();

assertEquals(string, new String(IOUtils.toBytes(decoded)));
```

The last bit can also be reversed: you can decode on read instead of on write. There is however one tricky detail to get right: a write uses the flush (or the close which always flushes) to indicate that you are done, this will trigger the generation of lead-out data.
The read however only knows that the data stream is done when it is closed which means we really need to close the decoded container here. Additionally because we are working in the opposite direction (read vs write) we need to reverse the transcoder order:

```java
decoded = IOUtils.wrap(
	TranscoderUtils.wrapReadable(decoded, new QuotedPrintableDecoder(QuotedPrintableEncoding.DEFAULT)),
	decoded
);
decoded = IOUtils.wrap(
	TranscoderUtils.wrapReadable(decoded, new GZIPDecoder()),
	decoded
);
```
	
A good rule of thumb is to always close the container when you are done writing, this works both with input and output decoding.

## Socket + SSL Example

This example can also be found in the testcases but is not a functioning unit test due to lack of a fixed proxy:

```java
public void testProxy() throws KeyManagementException, NoSuchAlgorithmException, IOException {
	// let's try google
	String host = "google.com";
	int port = 443;
	String proxy = "<proxy>";
	
	// first connect to a proxy
	Container<ByteBuffer> proxySocket = IOUtils.connect(proxy, 8080);
	
	// request a tunnel to host, using a copy because we want to add a wrapper that blocks until the data is written
	IOUtils.copyBytes(
		IOUtils.wrap(("CONNECT " + host + ":" + port + " HTTP/1.1\r\n"
				+ "Host: " + host + "\r\n"
				+ "Proxy-Connection: Keep-Alive\r\n"
				+ "\r\n").getBytes(), true),
		IOUtils.blockUntilWritten(proxySocket)
	);
	
	// let's check the reply of the proxy server, we should validate that it sends back HTTP/1.1 200 Connection Established
	System.out.println(new String(IOUtils.toBytes(proxySocket)));
	
	// starts ssl in client mode, note that in theory you can wrap the ssl layer around any Container, not necessarily a socket container
	Container<ByteBuffer> secureSocket = IOUtils.secure(proxySocket, createTrustAllContext(), true);
	
	// write a GET request for the root
	IOUtils.copyBytes(	
		IOUtils.wrap(("GET / HTTP/1.1\r\nHost: " + host + "\r\n"
				+ "\r\n").getBytes(), true), 
		IOUtils.blockUntilWritten(secureSocket)
	);
	
	// check the response of google
	// in my test it returned "HTTP/1.1 301 Moved Permanently" with a few more headers and a basic html page
	System.out.println(new String(IOUtils.toBytes(IOUtils.blockUntilRead(secureSocket))));

	secureSocket.close();
}
```

# Design Decisions

On the first attempt the library provided two sets of interfaces: one for bytes and one for characters.

```java
interface ReadableByteContainer {
	public int read(byte [] bytes);
	public int read(byte [] bytes, int offset, int length);
}

interface ReadableCharContainer {
	public int read(char [] chars);
	public int read(char [] chars, int offset, int length);
}
```

This is of course necessary because generics in java do not work with primitive types and wrapped types have too much overhead (Character [] != char [] overhead-wise)
However this made it harder to write generic utilities that perform basic I/O things, for example buffering had two implementations: BufferedReadableByteContainer and BufferedReadableCharContainer.
You can see how this can quickly become unmanageable as all utilities had to exist twice with very little shared code. To make things worse: it only supports bytes and chars, suppose you want to support something else? Duplicate!

The second attempt has solved this issue by solving the problem that plagues java nio: a generic "Buffer" has no read/write capability, this means that you can't actually write generic utilities necessitating a ReadableByteChannel instead of a ReadableChannel.
At the same time it addresses four other issues:

- no abstract base classes
- simpler interface for buffers
- buffers _are_ containers (container = channel in java nio)
- more state in buffers which means you don't have to flip() and compact() your way through data

To understand how this is solved, we need to look at some interfaces. First the ReadableContainer:

```java
public interface ReadableContainer<T extends Buffer<T>> extends Closeable {
	public long read(T buffer) throws IOException;
}
```

The WritableContainer:

```java
public interface WritableContainer<T extends Buffer<T>> extends Closeable {
	public long write(T buffer) throws IOException;
	public void flush() throws IOException;
}
```

And a Container interface that envelops them:

```java
public interface Container<T extends Buffer<T>> extends 
	ReadableContainer<T>, 
	WritableContainer<T> {}
```

You might argue that such a wrapper interface is not a good idea because if you have a class that implements (seperately) ReadableContainer & WritableContainer, it is still not a Container. This is however solved by the (rather large) utility class IOUtils which allows:

```java
Container<T> IOUtils.wrap(ReadableContainer<T> readable, WritableContainer<T> writable);
```

The truth is that in a _lot_ of cases you will be working with the Container interface so there needed to be a construct that allowed you to express it in code.

Back to the API: what is a Buffer?

```java
public interface Buffer<T extends Buffer<T>> extends 
		Container<T>, 
		LimitedReadableContainer<T>,
		LimitedWritableContainer<T>,
		TruncatableContainer<T>,
		SkippableContainer<T>,
		PeekableContainer<T> {

	public BufferFactory<T> getFactory();
	
}
```

As you can see, a Buffer is actually an advanced container with a few extra options. Each one of the options has proven itself absolutely necessary to write decent code. The last feature to be added was Peekable and I want to zoom in on that for a second.

```java
public interface PeekableContainer<T extends Buffer<T>> extends ReadableContainer<T> {
	public long peek(T buffer) throws IOException;
}
```

Peekable allows you to peek content from a container without actually updating the internal state. There needed to be a way to get content from a buffer without actually altering that buffer. 
Mark + reset is an option (and the interfaces for this exist) but it is usually a lot harder to be able to reset a pointer to a point in the past than to look ahead temporarily. Especially considering one of the main buffer implementations is cyclic.
One more question to ask is whether or not you want to be able to peek from a certain offset so you can peek bit-by-bit. This may be added in the future but currently it is not necessary and might prove burdensome on implementations.

While buffers are nice, we still have no way to actually read data into and out of a buffer, except by using another buffer of course which is equally useless.
As a sidenote you might find it odd and/or difficult to use generics of the type `T extends Buffer<T>` but luckily the outside developer practically never sees this because he uses specific types of buffers, e.g.:

```java
public interface ByteBuffer extends Buffer<ByteBuffer> {
	public int read(byte [] bytes, int offset, int length) throws IOException;
	public int write(byte [] bytes, int offset, int length) throws IOException;
	public int read(byte [] bytes) throws IOException;
	public int write(byte [] bytes) throws IOException;
}
```

Finally we have a way to actually put data into a buffer and read from it which we can then use to manipulate containers.
But what do we actually gain from all this generic madness? Generic utilities that don't care about the actual type of the data.
Because buffers can interact with other buffers of the same kind (in other words: a ByteBuffer can always interact with another ByteBuffer), we can actually write utilities that don't care what type of buffer you are using.
To enable truely generic utilities however the Buffer.getFactory() method is key: it allows internal creation of new buffers of whatever type you are using. This design (while not originally based on it) can also be found in org.jboss.netty.buffer.ChannelBuffer.

A few examples of I/O utilities that don't particularly care what type of buffer you are using:

- blocking containers: allow you to block until data is accessible, you can limit waiting in time or in minimum data amounts
- buffering containers
- synchronization containers: for thread-safe access
- counting containers: how much data has passed through a container
- fixed length containers: limits how much data can be read or written even if more is available
- pushback container
- multicast container
...

Additionally if you look at the utils-codec library that builds on this I/O API you can see the rather generic Transcoder interface:

```java
public interface Transcoder<T extends Buffer<T>> {
	public void transcode(ReadableContainer<T> in, WritableContainer<T> out) throws IOException;
	public void flush(WritableContainer<T> out) throws IOException;
}
```

In the first design there was actually a ByteTranscoder and it would've necessitated more duplication for other types.

On a sidenote, it appears that there are a few generic types of buffers that you absolutely need to make working with data easy (note that java.nio has exactly one such type)

- fixed size: the simplest of containers, it is usually backed by an array and simply keeps a pointer to where you were writing and reading. This "static" container usually offers a way to mark/reset or truncate to reread or rewrite but in general its contract is: write it full once and read it out once.
- cyclic fixed size: a slightly more difficult type of container that is also backed by an array but moves pointers in a cyclic fashion which means as long as you read enough, you can write indefinately.
- endless size: it allows you to write as much data as you want and read from it. The implementations that come with the library do clean up memory as you read data though so as long as you read enough, memory consumption will be acceptable.
	As opposed to say ByteArrayOutputStream it does _not_ use a single fixed array in the backend that has to be resized each time you get too large but instead keeps a whole bunch of non-resized backends.
	The only downside to this approach is potential cache misses but these appear to not matter as much because early performance tests indicated a 30% speed improvement.
	Additionally because it (by default) releases data that has already been read, the gc can take back piece by piece.
- sink: an often needed if sometimes overlooked member of the buffer family it is either limited in size (e.g. to skip specific amounts) or unlimited but it basically ignores all data. read() always returns 0.

The BufferFactory interface reflects these types:

```java
public interface BufferFactory<T extends Buffer<T>> {
	public T newInstance(long size, boolean cyclic);
	public T newInstance();
	public T newSink(long size);
}
```

Apart from that there are also wrappers from/to java.nio.buffers, java.io.streams, java.io.readers/writers, java.nio.channels.. allowing you to interact easily with existing java code. 
Note however that said wrappers always follow the spec: wrapping an InputStream to conform to this API doesn't magically make it non-blocking and alternatively wrapping a (potentially async) I/O source from this API in an InputStream will make it blocking.

# IOUtils

This library (like most of my libraries) has a heavy focus on interface-first (and only?) usage. This means you should be as unaware of actual implementations has humanly possible.
To this end there is the IOUtils class which offers a whole bunch of static methods that will do what you need it to do without you having to know specific implementations.
This has the added benefit that you only need to look in one place.

# Checked vs unchecked

In the first version there was an unchecked IORuntimeException. In the rewrite this was replaced with checked IOException.
I am still in doubt which is the better approach but have decided to follow the methodology of java I/O.

# Usage Guidelines

- If you actually need blocking I/O (e.g. your code can not deal with a partial write or an empty read), _do_ use java.io for all your code (and especially your method parameters).
	In case someone else is using this library and wants to call your code he can trivially wrap it.
- If you want to write blocking-agnostic utilities (but do think carefully on this, it is not always as easy as it seems), use the API.
- If you want to write non-blocking I/O: unless you have a specific reason to stick with java.nio, I would recommend this library as it has more flexibility. Also: trivial async SSL!

# To Do

1) The flush() in WritableContainer should return a long which should indicate if everything was already flushed (0) or it actually flushed something.
This would allow for large buffers to flush bit-by-bit to limited backends.

2) Perhaps add a method "isBlocking()" to all containers.

3) Add @java.lang.SafeVarargs where necessary once 8 is the standard