package ledcontrol.runner;

import static java.awt.Color.BLACK;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static ledcontrol.arrays.Arrays.rotate;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.IntStream;

import ledcontrol.Proto;
import ledcontrol.connection.SerialConnection;

public class DemoRunner {

	private static final Color[] fgad_farben = new Color[] { new Color(0, 102, 179), new Color(138, 192, 86),
			new Color(160, 80, 154), new Color(255, 102, 0), new Color(209, 10, 69), new Color(86, 188, 177),
			new Color(250, 203, 65) };

	public static void main(String[] args) throws IOException, InterruptedException {

		SECONDS.sleep(2);

		SerialConnection connection = new SerialConnection("/dev/ttyUSB4", 230400);
		BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));

		int ledCount = 120;
//		Proto proto = Proto.forFrameSizes(connection.getOutputStream(), 40, 40, 40);
		Proto proto = Proto.forFrameSizes(connection.getOutputStream(), ledCount);

		Color[] leds = initLeds(ledCount, 5);
		while (true) {
			proto.write(leds);
			rotate(Color.class, leds, 1);
			MILLISECONDS.sleep(40);
			read(br);
		}
	}

	private static Color[] initLeds(int leds, int breite) {
		Color[] colors = IntStream.range(0, leds).mapToObj(i -> BLACK).toArray(Color[]::new);
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
