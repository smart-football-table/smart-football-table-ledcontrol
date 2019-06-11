package ledcontrol.arrays;

import static ledcontrol.arrays.Arrays.rotate;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.jupiter.api.Test;

public class ArraysTest {

	@Test
	void testRotateSingle() {
		assertThat(rotate(String.class, new String[] { "A" }, 1), is(new String[] { "A" }));
	}

	@Test
	void testRotate() {
		assertThat(rotate(String.class, new String[] { "A", "B", "C" }, 1), is(new String[] { "C", "A", "B" }));
	}

	@Test
	void testRotate2() {
		assertThat(rotate(String.class, new String[] { "A", "B", "C" }, 2), is(new String[] { "B", "C", "A" }));
	}

}
