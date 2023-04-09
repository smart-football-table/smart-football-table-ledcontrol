package com.github.smartfootballtable.ledcontrol.arrays;

import static com.github.smartfootballtable.ledcontrol.arrays.Arrays.rotate;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.jupiter.api.Test;

class ArraysTest {

	private static final String[] ROTATE_BY_ZERO = new String[] { "A", "B", "C" };
	private static final String[] ROTATE_BY_ONE = new String[] { "C", "A", "B" };

	String[] elements = new String[] { "A", "B", "C" };

	@Test
	void testRotateSingle() {
		elements = new String[] { "A" };
		rotate(elements, 1);
		assertThat(elements, is(new String[] { "A" }));
	}

	@Test
	void testRotate0() {
		rotate(elements, 0);
		assertThat(elements, is(ROTATE_BY_ZERO));
	}

	@Test
	void testRotateArrayLength() {
		rotate(elements, elements.length);
		assertThat(elements, is(ROTATE_BY_ZERO));
	}

	@Test
	void testRotate() {
		rotate(elements, 1);
		assertThat(elements, is(ROTATE_BY_ONE));
	}

	@Test
	void testRotateArrayLengthPlusOne() {
		rotate(elements, elements.length + 1);
		assertThat(elements, is(ROTATE_BY_ONE));
	}

	@Test
	void testRotate2() {
		rotate(elements, 2);
		assertThat(elements, is(new String[] { "B", "C", "A" }));
	}

}
