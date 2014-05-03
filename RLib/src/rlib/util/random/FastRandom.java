package rlib.util.random;

/**
 * Реализация быстрого рандоминайзера на основе псевдорандом генератора.
 * 
 * @author Ronn
 */
public final class FastRandom implements Random {

	/** генератор чисел */
	private final java.util.Random random;

	public FastRandom() {
		this.random = new java.util.Random();
	}

	@Override
	public void byteArray(final byte[] array, final int offset, int length) {

		length += offset;

		for(int i = offset; i < length; i++) {
			array[i] = (byte) nextInt(256);
		}
	}

	@Override
	public boolean chance(final float chance) {

		if(chance < 0F) {
			return false;
		}

		if(chance > 99.999999F) {
			return true;
		}

		return nextFloat() * nextInt(100) <= chance;
	}

	@Override
	public boolean chance(final int chance) {

		if(chance < 1) {
			return false;
		}

		if(chance > 99) {
			return true;
		}

		return nextInt(99) <= chance;
	}

	@Override
	public float nextFloat() {
		return random.nextFloat();
	}

	@Override
	public int nextInt() {
		return random.nextInt();
	}

	@Override
	public int nextInt(final int max) {
		return random.nextInt(max);
	}

	@Override
	public int nextInt(final int min, final int max) {
		return min + nextInt(Math.abs(max - min) + 1);
	}

	@Override
	public long nextLong(final long min, final long max) {
		return min + Math.round(nextFloat() * Math.abs(max - min) + 1);
	}
}
