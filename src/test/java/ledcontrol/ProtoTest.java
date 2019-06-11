package ledcontrol;

import static java.awt.Color.BLUE;
import static java.awt.Color.GREEN;
import static java.awt.Color.ORANGE;
import static java.awt.Color.RED;
import static java.util.Collections.addAll;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class ProtoTest {

	private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

	@Test
	void canControlSingleLed() throws IOException {
		whenSwitchingLeds(RED);
		thenBytesWritten(1, RED);
	}

	@Test
	void canControlMultipleLeds() throws IOException {
		whenSwitchingLeds(RED, GREEN, BLUE);
		thenBytesWritten(1, RED, GREEN, BLUE);
	}

	@Test
	void canChunck() throws IOException {
		whenSwitchingLedsUsingTwoChuncks(RED, GREEN, BLUE, ORANGE);
		thenBytesWritten(2, RED, GREEN, BLUE, ORANGE);
	}

	private void thenBytesWritten(int expectedTotalPakets, Color... expectedColors) throws IOException {
		ByteArrayInputStream is = new ByteArrayInputStream(outputStream.toByteArray());

		List<Color> readColors = new ArrayList<>();
		int currentPaket = 0;
		do {
			Tpm2Frame frame = Tpm2Frame.fromStream(is);
			assertThat(frame.getCurrentPaket(), is(currentPaket++));
			assertThat(frame.getTotalPaket(), is(expectedTotalPakets));
			addAll(readColors, frame.getColors());
		} while (readColors.size() < expectedColors.length);

		assertThat(readColors, is(Arrays.asList(expectedColors)));
	}

	private void whenSwitchingLeds(Color... colors) throws IOException {
		try (Proto proto = Proto.forFrameSizes(outputStream, colors.length)) {
			proto.write(colors);
		}
	}

	private void whenSwitchingLedsUsingTwoChuncks(Color... colors) throws IOException {
		try (Proto proto = Proto.forFrameSizes(outputStream, 1, 3)) {
			proto.write(colors);
		}
	}

}
