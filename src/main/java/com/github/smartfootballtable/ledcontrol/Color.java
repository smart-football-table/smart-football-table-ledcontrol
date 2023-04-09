package com.github.smartfootballtable.ledcontrol;

public class Color {

	public static final Color WHITE = new Color(255, 255, 255);
	public static final Color LIGHT_GRAY = new Color(192, 192, 192);
	public static final Color GRAY = new Color(128, 128, 128);
	public static final Color DARK_GRAY = new Color(64, 64, 64);
	public static final Color BLACK = new Color(0, 0, 0);
	public static final Color RED = new Color(255, 0, 0);
	public static final Color PINK = new Color(255, 175, 175);
	public static final Color ORANGE = new Color(255, 200, 0);
	public static final Color YELLOW = new Color(255, 255, 0);
	public static final Color GREEN = new Color(0, 255, 0);
	public static final Color MAGENTA = new Color(255, 0, 255);
	public static final Color CYAN = new Color(0, 255, 255);
	public static final Color BLUE = new Color(0, 0, 255);

	private final int value;

	public static Color decode(String nm) throws NumberFormatException {
		int intval = Integer.decode(nm).intValue();
		return new Color((intval >> 16) & 0xFF, (intval >> 8) & 0xFF, intval & 0xFF);
	}

	public Color(int r, int g, int b) {
		value = ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | ((b & 0xFF) << 0);
	}

	public int getRed() {
		return getRGB() >> 16 & 0xFF;
	}

	public int getGreen() {
		return getRGB() >> 8 & 0xFF;
	}

	public int getBlue() {
		return getRGB() >> 0 & 0xFF;
	}

	public int getRGB() {
		return value;
	}

	public int hashCode() {
		return value;
	}

	public boolean equals(Object obj) {
		return obj instanceof Color && ((Color) obj).value == value;
	}

	public String toString() {
		return getClass().getName() + "[r=" + getRed() + ",g=" + getGreen() + ",b=" + getBlue() + "]";
	}

}
