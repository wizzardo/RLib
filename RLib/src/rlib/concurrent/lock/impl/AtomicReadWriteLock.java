package rlib.concurrent.lock.impl;

import org.jetbrains.annotations.NotNull;

import sun.misc.Contended;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import rlib.concurrent.atomic.AtomicInteger;
import rlib.concurrent.lock.AsyncReadSyncWriteLock;

/**
 * The implementation of the {@link AsyncReadSyncWriteLock} using the several {@link AtomicInteger} without supporting
 * reentrant calls.
 *
 * @author JavaSaBr
 */
public class AtomicReadWriteLock implements AsyncReadSyncWriteLock, Lock {

    private static final int STATUS_WRITE_LOCKED = 1;
    private static final int STATUS_WRITE_UNLOCKED = 0;

    private static final int STATUS_READ_UNLOCKED = 0;
    private static final int STATUS_READ_LOCKED = -200000;

    /**
     * The status of write lock.
     */
    @Contended("writeStatus")
    protected final AtomicInteger writeStatus;

    /**
     * The count of writers.
     */
    @Contended("writeCount")
    protected final AtomicInteger writeCount;

    /**
     * The count of readers.
     */
    @Contended("readCount")
    protected final AtomicInteger readCount;

    /**
     * The field for consuming CPU.
     */
    protected int sink;

    public AtomicReadWriteLock() {
        this.writeCount = new AtomicInteger(0);
        this.writeStatus = new AtomicInteger(0);
        this.readCount = new AtomicInteger(0);
        this.sink = 1;
    }

    @Override
    public void asyncLock() {
        while (!tryReadLock()) consumeCPU();
    }

    @Override
    public void asyncUnlock() {
        readCount.decrementAndGet();
    }

    protected void consumeCPU() {

        final int value = sink;
        int newValue = value * value;
        newValue += value >>> 1;
        newValue += value & newValue;
        newValue += value >>> 1;
        newValue += value & newValue;
        newValue += value >>> 1;
        newValue += value & newValue;

        sink = newValue;
    }

    @Override
    public void lock() {
        syncLock();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public Condition newCondition() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void syncLock() {
        writeCount.incrementAndGet();
        while (tryToLockReading()) consumeCPU();
        while (tryToLockWriting()) consumeCPU();
    }

    protected boolean tryToLockWriting() {
        return writeStatus.get() != STATUS_WRITE_UNLOCKED || !writeStatus.compareAndSet(STATUS_WRITE_UNLOCKED, STATUS_WRITE_LOCKED);
    }

    protected boolean tryToLockReading() {
        return readCount.get() != STATUS_READ_UNLOCKED || !readCount.compareAndSet(STATUS_READ_UNLOCKED, STATUS_READ_LOCKED);
    }

    @Override
    public void syncUnlock() {
        writeStatus.set(STATUS_WRITE_UNLOCKED);
        readCount.set(STATUS_READ_UNLOCKED);
        writeCount.decrementAndGet();
    }

    @Override
    public boolean tryLock() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean tryLock(final long time, @NotNull final TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    /**
     * Try to get read lock.
     */
    private boolean tryReadLock() {
        if (writeCount.get() != 0) return false;
        final int value = readCount.get();
        return value != STATUS_READ_LOCKED && readCount.compareAndSet(value, value + 1);
    }

    @Override
    public void unlock() {
        syncUnlock();
    }

    @Override
    public String toString() {
        return "AtomicReadWriteLock{" +
                "readCount=" + readCount +
                ", writeCount=" + writeCount +
                ", writeStatus=" + writeStatus +
                '}';
    }
}
