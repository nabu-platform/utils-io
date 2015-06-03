package be.nabu.utils.io.buffers;

import java.io.IOException;

import be.nabu.utils.io.api.Buffer;
import be.nabu.utils.io.api.BufferFactory;

public class ShardedBuffer<T extends Buffer<T>> extends CompositeBuffer<T> {
	
	public ShardedBuffer(BufferFactory<T> factory, boolean fifo) {
		super(factory, fifo, false);
	}

	@Override
	public long write(T buffer) throws IOException {
		if (buffers.isEmpty()) {
			shard();
		}
		Buffer<T> target = buffers.get(buffers.size() - 1);
		return target.write(buffer);
	}
	
	public void shard() {
		buffers.add(factory.newInstance());
	}

}
