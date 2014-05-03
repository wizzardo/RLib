package rlib.idfactory.impl;

import java.util.concurrent.atomic.AtomicInteger;

import rlib.idfactory.IdGenerator;

/**
 * Модель простого генератора ид.
 * 
 * @author Ronn
 */
public final class SimpleIdGenerator implements IdGenerator {

	/** промежуток генерирование ид */
	private final int start;
	private final int end;

	/** следующий ид */
	private final AtomicInteger nextId;

	/**
	 * @param start стартовый ид генератора.
	 * @param end конечный ид генератора.
	 */
	public SimpleIdGenerator(final int start, final int end) {
		this.start = start;
		this.end = end;
		this.nextId = new AtomicInteger(start);
	}

	@Override
	public int getNextId() {
		nextId.compareAndSet(end, start);
		return nextId.incrementAndGet();
	}

	@Override
	public int usedIds() {
		return nextId.get() - start;
	}
}
