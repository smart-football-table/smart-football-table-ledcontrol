package ledcontrol.panel;

import static java.util.Arrays.stream;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import ledcontrol.panel.StackedPanel.OverlayStrategy;

public class Panel {

	public interface RepaintListener {
		void repaint(Panel panel);
	}

	private final int width;
	private final int height;
	protected final Color[][] colors;
	private Color transparent;
	private final List<RepaintListener> repaintListeners = new CopyOnWriteArrayList<>();
	private OverlayStrategy overlayStrategy;

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

	public void copyTo(Panel target) {
		Color[][] colors = getColors();
		for (int y = 0; y < colors.length; y++) {
			for (int x = 0; x < colors[y].length; x++) {
				Color newColor = colors[y][x];
				overlayStrategy.copy(x, y, newColor, target);
			}
		}
	}

	public void addRepaintListener(RepaintListener listener) {
		this.repaintListeners.add(listener);
	}

	public void setOverlayStrategy(OverlayStrategy overlayStrategy) {
		this.overlayStrategy = overlayStrategy;
	}

}