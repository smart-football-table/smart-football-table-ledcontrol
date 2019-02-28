package ledcontrol.panel;

import static java.util.Arrays.stream;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Panel {

	public interface RepaintListener {
		void repaint(Panel panel);
	}

	private final int width;
	private final int height;
	protected final Color[][] colors;
	private final List<RepaintListener> repaintListeners = new CopyOnWriteArrayList<>();

	public Panel(final int width, final int height) {
		this.width = width;
		this.height = height;
		this.colors = new Color[height][width];
	}

	public Panel(final int width, final int height, Color color) {
		this(width, height);
		fill(color);
	}

	public Panel fill(Color color) {
		stream(colors).forEach(arr -> Arrays.fill(arr, color));
		return this;
	}

	public Panel clear() {
		fill(null);
		return this;
	}

	public Color[][] getColors() {
		return colors;
	}

	public Panel setColor(final int x, final int y, final Color color) {
		colors[y][x] = color;
		return this;
	}

	public Panel repaint() {
		for (RepaintListener listener : repaintListeners) {
			listener.repaint(this);
		}
		return this;
	}

	public int getHeight() {
		return height;
	}

	public int getWidth() {
		return width;
	}

	public void addRepaintListener(RepaintListener listener) {
		this.repaintListeners.add(listener);
	}

}