package com.github.smartfootballtable.ledcontrol;

import static com.github.smartfootballtable.ledcontrol.LedControl.FPS.framesPerSecond;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.jupiter.api.Test;

class FPSTest {	

	@Test
	void fpsSleepTime() {
		assertThat(framesPerSecond(1).sleepTime(MILLISECONDS), is(1_000L));
		assertThat(framesPerSecond(1).sleepTime(SECONDS), is(1L));

		assertThat(framesPerSecond(2).sleepTime(MILLISECONDS), is(500L));
		assertThat(framesPerSecond(2).sleepTime(SECONDS), is(0L));
	}

}
