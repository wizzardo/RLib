package rlib.util.pools;

import rlib.util.array.Array;
import rlib.util.array.ArrayFactory;

/**
 * Не потокобезопасный пул.
 * 
 * @author Ronn
 */
public class FastPool<E> implements Pool<E> {

	/** пул объектов */
	private final Array<E> pool;

	protected FastPool(final int size, final Class<?> type) {
		this.pool = ArrayFactory.newArray(type, size);
	}

	@Override
	public boolean isEmpty() {
		return pool.isEmpty();
	}

	@Override
	public void put(final E object) {

		if(object == null) {
			return;
		}

		pool.add(object);
	}

	@Override
	public void remove(final E object) {
		pool.fastRemove(object);
	}

	@Override
	public E take() {

		final E object = pool.pop();

		if(object == null) {
			return null;
		}

		return object;
	}

	@Override
	public String toString() {
		return pool.toString();
	}
}
