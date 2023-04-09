package com.github.smartfootballtable.ledcontrol.runner;

import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemErr;
import static com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.jupiter.api.Test;

class SystemRunnerTest {

	@Test
	void printsHelpOnMinusH() throws Exception {
		SystemRunner systemRunner = new SystemRunner();

		assertThat(tapSystemErr(() -> {
			assertThat(systemRunner.parseArgs("-h"), is(false));
		}), allOf(//
				containsString("-baudrate "), //
				containsString("-leds "), //
				containsString("-mqttHost "), //
				containsString("-mqttPort "), //
				containsString("-tty ")));
	}

	@Test
	void canReadEnvVars() throws Exception {
		SystemRunner systemRunner = new SystemRunner();
		withEnvironmentVariable("BAUDRATE", "1")//
				.and("LEDS", "2")//
				.and("MQTTHOST", "someMqttHost")//
				.and("MQTTPORT", "3") //
				.and("TTY", "someTTY").execute(() -> {
					assertThat(systemRunner.parseArgs(), is(true));
					assertThat(systemRunner.baudrate, is(1));
					assertThat(systemRunner.leds, is(2));
					assertThat(systemRunner.mqttHost, is("someMqttHost"));
					assertThat(systemRunner.mqttPort, is(3));
					assertThat(systemRunner.tty, is("someTTY"));
				});
	}

}
