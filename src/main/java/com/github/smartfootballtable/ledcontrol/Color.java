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

	private static final int SHIFT_RED = 2;
	private static final int SHIFT_GREEN = 1;
	private static final int SHIFT_BLUE = 0;

	private static final int BITS_PER_BYTE = 8;
	private static final int _0X_FF = 0xFF;

	private final int value;

	public static Color decode(String nm) throws NumberFormatException {
		int value = Integer.decode(nm).intValue();
		return new Color(shiftRight(value, SHIFT_RED), shiftRight(value, SHIFT_GREEN), shiftRight(value, 0));
	}

	public Color(int r, int g, int b) {
		value = shiftLeft(r, SHIFT_RED) | shiftLeft(g, SHIFT_GREEN) | shiftLeft(b, SHIFT_BLUE);
	}

	public int getRed() {
		return shiftRight(value, SHIFT_RED);
	}

	public int getGreen() {
		return shiftRight(value, SHIFT_GREEN);
	}

	public int getBlue() {
		return shiftRight(value, SHIFT_BLUE);
	}

	private static int shiftLeft(int value, int bytes) {
		return (value & _0X_FF) << bytes * BITS_PER_BYTE;
	}

	private static int shiftRight(int value, int bytes) {
		return value >> bytes * BITS_PER_BYTE & _0X_FF;
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
