package ledcontrol.arrays;

import java.lang.reflect.Array;

public final class Arrays {

	private Arrays() {
		super();
	}

	/**
	 * Rotate the elements in the array by the passed amount.
	 * 
	 * @param <T>      type of array
	 * @param elements elements to rotate
	 * @param by       the amount to rotate
	 */
	public static <T> void rotate(T[] elements, int by) {
		by = by % elements.length;
		@SuppressWarnings("unchecked")
		T[] temp = (T[]) Array.newInstance(elements.getClass().getComponentType(), by);
		System.arraycopy(elements, elements.length - by, temp, 0, by);
		System.arraycopy(elements, 0, elements, by, elements.length - by);
		System.arraycopy(temp, 0, elements, 0, by);
	}

}
