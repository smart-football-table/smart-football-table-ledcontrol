package ledcontrol;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class TpmDecode {

	public static List<Tpm2Frame> decodeFrames(byte[] bytes) throws IOException {
		InputStream is = new ByteArrayInputStream(bytes);
		List<Tpm2Frame> frames = new ArrayList<Tpm2Frame>();
		while (is.available() > 0) {
			frames.add(Tpm2Frame.fromStream(is));
		}
		return frames;
	}

	public static Color[][] toColors(Tpm2Frame frame, int height, int width) {
		Color[][] colors = new Color[height][width];
		for (int y = 0; y < height; y++) {
			System.arraycopy(frame.getColors(), y * width, colors[y], 0, width);
		}
		return colors;
	}

}
