package rlib.util.array.impl;

import rlib.concurrent.lock.AsyncReadSyncWriteLock;
import rlib.concurrent.lock.LockFactory;
import rlib.util.array.ConcurrentArray;

/**
 * The concurrent implementation of the array using {@link LockFactory#newARSWLock()} for {@link
 * ConcurrentArray#readLock()} and {@link ConcurrentArray#writeLock()}.
 *
 * @author JavaSaBr
 */
public class ConcurrentPrimitiveAtomicARSWLockArray<E> extends AbstractConcurrentArray<E> {

    private static final long serialVersionUID = -6291504312637658721L;

    /**
     * The locker.
     */
    private final AsyncReadSyncWriteLock lock;

    public ConcurrentPrimitiveAtomicARSWLockArray(final Class<E> type) {
        this(type, 10);
    }

    public ConcurrentPrimitiveAtomicARSWLockArray(final Class<E> type, final int size) {
        super(type, size);

        this.lock = LockFactory.newPrimitiveAtomicARSWLock();
    }

    @Override
    public final long readLock() {
        lock.asyncLock();
        return 0;
    }

    @Override
    public void readUnlock(final long stamp) {
        lock.asyncUnlock();
    }

    @Override
    public final long writeLock() {
        lock.syncLock();
        return 0;
    }

    @Override
    public void writeUnlock(final long stamp) {
        lock.syncUnlock();
    }
}