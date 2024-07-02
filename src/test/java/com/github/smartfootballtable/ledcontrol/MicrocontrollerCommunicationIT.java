package com.github.smartfootballtable.ledcontrol;

import static com.github.smartfootballtable.ledcontrol.virtualavr.VirtualAvrConnection.connectionToVirtualAvr;
import static java.awt.Color.BLACK;
import static java.awt.Color.BLUE;
import static java.awt.Color.GREEN;
import static java.awt.Color.RED;
import static java.awt.Color.WHITE;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.testcontainers.containers.BindMode.READ_ONLY;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.github.smartfootballtable.ledcontrol.virtualavr.VirtualAvrConnection;

import ledcontrol.Proto;
import ledcontrol.connection.SerialConnection;

@Testcontainers(disabledWithoutDocker = true)
public class MicrocontrollerCommunicationIT {

	String hostDev = "/dev";
	String containerDev = "/dev";
	String ttyDevice = "ttyUSB0";

	String firmware = "sketches/tpm2serial/tpm2serial.ino";

	@Container
	GenericContainer<?> virtualavr = new GenericContainer<>("pfichtner/virtualavr")
			.withEnv("VIRTUALDEVICE", containerDev + "/" + ttyDevice) //
			.withFileSystemBind(hostDev, containerDev) //
			.withFileSystemBind(firmware, "/app/code.ino", READ_ONLY) //
			.withExposedPorts(8080) //
	;

	@Test
	void xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx() throws IOException, InterruptedException {
		SerialConnection connection = new SerialConnection(hostDev + "/" + ttyDevice, 230400);
		VirtualAvrConnection virtualAvrConnection = connectionToVirtualAvr(virtualavr);

		Proto proto = Proto.forFrameSizes(connection.getOutputStream(), 5 * 1);
		proto.write(WHITE, BLACK, RED, GREEN, BLUE);

		SECONDS.sleep(20);

		InputStream in = connection.getInputStream();
		new Thread() {
			{
				start();
			}
			@Override
			public void run() {
				try {
					while (in.available() > 0) {
						System.out.print((char) in.read());
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};

		System.out.println(virtualAvrConnection.pinStates());

		// TODO if we could attach WS2812B leds to the virtualavr we could query if led0
		// is white, led1 is black and so on

		// TODO because we cannot attach WS2812B leds to the virtualavr yet we do
		// approval tests if the virtualavr received the same frames then before.

		// TODO do await with approvals

//			await().until(() -> count(virtualAvrCon.pinStates(), switchedOn(INTERNAL_LED)) >= 3
//					&& count(virtualAvrCon.pinStates(), switchedOff(INTERNAL_LED)) >= 3);
	}
}
