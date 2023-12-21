package com.github.smartfootballtable.ledcontrol.panel;

import static java.util.Arrays.stream;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import com.github.smartfootballtable.ledcontrol.Color;

public class Panel {

	public interface RepaintListener {
		void repaint(Panel panel);
	}

	public static interface OverlayStrategy {

		OverlayStrategy DEFAULT = transparentOn(null);

		static OverlayStrategy transparentOn(Color transparentColor) {
			return (x, y, newColor, target) -> {
				if (!Objects.equals(transparentColor, newColor)) {
					target.setColor(x, y, newColor);
				}
			};
		}

		void copy(int x, int y, Color color, Panel target);

	}

	private final int width;
	private final int height;
	protected final Color[][] colors;
	private final List<RepaintListener> repaintListeners = new CopyOnWriteArrayList<>();
	private Panel.OverlayStrategy overlayStrategy = OverlayStrategy.DEFAULT;

	public Panel(int width, int height) {
		this.width = width;
		this.height = height;
		this.colors = new Color[height][width];
	}

	public Panel(int width, int height, Color color) {
		this(width, height);
		fill(color);
	}

	public Panel fill(Color color) {
		stream(colors).forEach(arr -> Arrays.fill(arr, color));
		return this;
	}

	public int getHeight() {
		return height;
	}

	public int getWidth() {
		return width;
	}

	public Panel clear() {
		fill(null);
		return this;
	}

	public Color[][] getColors() {
		return colors;
	}

	public Panel setColor(int x, int y, Color color) {
		colors[y][x] = color;
		return this;
	}

	public Panel repaint() {
		for (RepaintListener listener : repaintListeners) {
			listener.repaint(this);
		}
		return this;
	}

	public void copyTo(Panel target) {
		Color[][] sourceColors = getColors();
		for (int y = 0; y < sourceColors.length; y++) {
			for (int x = 0; x < sourceColors[y].length; x++) {
				Color sourceColor = sourceColors[y][x];
				overlayStrategy.copy(x, y, sourceColor, target);
			}
		}
	}

	public Panel addRepaintListener(RepaintListener listener) {
		this.repaintListeners.add(listener);
		return this;
	}

	public Panel overlayStrategy(OverlayStrategy overlayStrategy) {
		this.overlayStrategy = overlayStrategy;
		return this;
	}

}