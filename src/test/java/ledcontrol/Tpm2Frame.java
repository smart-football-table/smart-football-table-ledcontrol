package ledcontrol;

import static ledcontrol.util.Preconditions.checkState;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.IntStream;

public class Tpm2Frame {

	private final int currentPaket;
	private final int totalPaket;
	private final Color[] colors;

	public static Tpm2Frame fromStream(InputStream is) throws IOException {
		return new Tpm2Frame(is);
	}

	private Tpm2Frame(InputStream is) throws IOException {
		int packetStartByte = is.read();
		checkState(packetStartByte == 0x9C, "PacketStartByte 0x9C expected but got %s", packetStartByte);
		int packetType = is.read();
		checkState(packetType == 0xDA, "PacketType 0xDA expected but got %s", packetType);
		int size = (is.read() << 8) | is.read() / 3;

		this.currentPaket = is.read();
		this.totalPaket = is.read();

		this.colors = readColors(is, size);
		int packetEndByte = is.read();
		checkState(packetEndByte == 0x36, "PacketStartByte 0x36 expected but got %s", packetEndByte);
	}

	private Color[] readColors(InputStream is, int size) {
		return IntStream.range(0, size).mapToObj(n -> {
			try {
				return new Color(is.read(), is.read(), is.read());
			} catch (IOException e) {
				throw new RuntimeException("Error creating color", e);
			}
		}).toArray(Color[]::new);
	}

	public int getCurrentPaket() {
		return currentPaket;
	}

	public int getTotalPaket() {
		return totalPaket;
	}

	public Color[] getColors() {
		return colors;
	}

}