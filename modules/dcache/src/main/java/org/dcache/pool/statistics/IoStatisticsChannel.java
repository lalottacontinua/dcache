package org.dcache.pool.statistics;

import org.dcache.pool.repository.RepositoryChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.SyncFailedException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * This class decorates any RepositoryChannel and adds the function of collecting data for the IoStatistics
 * Hint: It might be interesting for further developments to have a closer look at return values from read and write methods, when they equal 0
 */
public class IoStatisticsChannel implements RepositoryChannel {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(IoStatisticsChannel.class);

    /**
     * Inner channel to which all operations are delegated.
     */
    private RepositoryChannel channel;

    private final IoStatistics statistics = new IoStatistics();

    public IoStatisticsChannel(RepositoryChannel channel){ channel = channel; }

    /**
     * Returns the object most central of this decorator
     * @return object with collected and evaluated statistics data
     */
    public IoStatistics getStatistics() {
        return statistics;
    }

    @Override
    public int write(ByteBuffer buffer, long position) throws IOException {
        long startTime = System.nanoTime();
        int writtenBytes = channel.write(buffer, position); // might be 0 if nothing has been written
        long duration = System.nanoTime() - startTime;
        statistics.updateWrite(writtenBytes, duration);
        return writtenBytes;
    }

    @Override
    public int read(ByteBuffer buffer, long position) throws IOException {
        long startTime = System.nanoTime();
        int readBytes = channel.read(buffer, position); // -1 => position greater than file; 0 => if end of file
        long duration = System.nanoTime() - startTime;
        statistics.updateRead(readBytes, duration);
        return readBytes;
    }

    @Override
    public void sync() throws SyncFailedException, IOException {
        channel.sync();
    }

    @Override
    public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
        long startTime = System.nanoTime();
        long readBytes =  channel.transferTo(position, count, target);
        long duration = System.nanoTime() - startTime;
        statistics.updateRead(readBytes, duration);
        return readBytes;
    }

    @Override
    public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
        long startTime = System.nanoTime();
        long writtenBytes =  channel.transferFrom(src, position, count);
        long duration = System.nanoTime() - startTime;
        statistics.updateWrite(writtenBytes, duration);
        return writtenBytes;
    }

    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        long startTime = System.nanoTime();
        long writtenBytes = channel.write(srcs, offset, length);
        long duration = System.nanoTime() - startTime;
        statistics.updateWrite(writtenBytes, duration);
        return writtenBytes;
    }

    @Override
    public long write(ByteBuffer[] srcs) throws IOException {
        long startTime = System.nanoTime();
        long writtenBytes = channel.write(srcs);
        long duration = System.nanoTime() - startTime;
        statistics.updateWrite(writtenBytes, duration);
        return writtenBytes;
    }

    @Override
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        long startTime = System.nanoTime();
        long readBytes = channel.read(dsts, offset, length);
        long duration = System.nanoTime() - startTime;
        statistics.updateRead(readBytes, duration);
        return readBytes;
    }

    @Override
    public long read(ByteBuffer[] dsts) throws IOException {
        long startTime = System.nanoTime();
        long readBytes = channel.read(dsts);
        long duration = System.nanoTime() - startTime;
        statistics.updateRead(readBytes, duration);
        return readBytes;
    }

    @Override
    public long position() throws IOException {
        return channel.position();
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        return channel.position(newPosition);
    }

    @Override
    public long size() throws IOException {
        return channel.size();
    }

    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        return channel.truncate(size);
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        long startTime = System.nanoTime();
        int readBytes = channel.read(dst);
        long duration = System.nanoTime() - startTime;
        statistics.updateRead(readBytes, duration);
        return readBytes;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        long startTime = System.nanoTime();
        int writtenBytes = channel.write(src);
        long duration = System.nanoTime() - startTime;
        statistics.updateWrite(writtenBytes, duration);
        return writtenBytes;
    }

    @Override
    public boolean isOpen() {
        return channel.isOpen();
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }
}
