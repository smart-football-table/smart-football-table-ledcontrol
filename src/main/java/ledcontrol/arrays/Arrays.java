package ledcontrol.arrays;

import java.lang.reflect.Array;

public final class Arrays {
	
	private Arrays() {
		super();
	}

	public static <T> T[] rotate(Class<T> clazz, T[] arr, int a) {
		@SuppressWarnings("unchecked")
		T[] temp = (T[]) Array.newInstance(clazz, a);
		System.arraycopy(arr, arr.length - a, temp, 0, a);
		System.arraycopy(arr, 0, arr, a, arr.length - a);
		System.arraycopy(temp, 0, arr, 0, a);
		return arr;
	}

}
