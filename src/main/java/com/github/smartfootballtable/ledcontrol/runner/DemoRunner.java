package com.github.smartfootballtable.ledcontrol.runner;

import static com.github.smartfootballtable.ledcontrol.Color.BLACK;
import static com.github.smartfootballtable.ledcontrol.arrays.Arrays.rotate;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Stream.generate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.github.smartfootballtable.ledcontrol.Color;
import com.github.smartfootballtable.ledcontrol.Proto;
import com.github.smartfootballtable.ledcontrol.connection.SerialConnection;

public class DemoRunner {

	private static final String DEV_TTY_USB = "/dev/ttyUSB4";
	private static final int BAUDRATE = 230400;

	private static final Color[] fgad_farben = new Color[] { new Color(0, 102, 179), new Color(138, 192, 86),
			new Color(160, 80, 154), new Color(255, 102, 0), new Color(209, 10, 69), new Color(86, 188, 177),
			new Color(250, 203, 65) };

	public static void main(String[] args) throws IOException, InterruptedException {

		SECONDS.sleep(2);

		SerialConnection connection = new SerialConnection(DEV_TTY_USB, BAUDRATE);
		BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));

		int ledCount = 120;
//		Proto proto = Proto.forFrameSizes(connection.getOutputStream(), 40, 40, 40);
		Proto proto = Proto.forFrameSizes(connection.getOutputStream(), ledCount);

		Color[] leds = initLeds(ledCount, 5);
		while (!Thread.currentThread().isInterrupted()) {
			proto.write(leds);
			// we could introduce a shifting list but currently it is used only here in the
			// DemoRunner
			rotate(leds, 1);
			MILLISECONDS.sleep(40);
			read(br);
		}
	}

	private static Color[] initLeds(int leds, int breite) {
		Color[] colors = generate(() -> BLACK).limit(leds).toArray(Color[]::new);
		for (int start = 0; start < leds; start += 60) {
			for (int i = 0; i < fgad_farben.length; i++) {
				int base = start + i * breite;
				Color color = fgad_farben[i];
				for (int a = 0; a < breite; a++) {
					colors[base + a] = color;
				}
			}
		}
		return colors;
	}

	private static void read(BufferedReader reader) throws IOException {
		while (reader.ready()) {
			System.out.println("<- " + reader.readLine());
		}
	}

}
