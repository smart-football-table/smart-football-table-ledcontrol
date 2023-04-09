package com.github.smartfootballtable.ledcontrol;

public class Color {

	private static final int BITS_PER_BYTE = 8;
	private static final int MIN_BYTE = 0x00;
	private static final int MAX_BYTE = 0xFF;

	public static final Color WHITE = new Color(MAX_BYTE, MAX_BYTE, MAX_BYTE);
	public static final Color LIGHT_GRAY = new Color(192, 192, 192);
	public static final Color GRAY = new Color(128, 128, 128);
	public static final Color DARK_GRAY = new Color(64, 64, 64);
	public static final Color BLACK = new Color(MIN_BYTE, MIN_BYTE, MIN_BYTE);
	public static final Color RED = new Color(MAX_BYTE, MIN_BYTE, MIN_BYTE);
	public static final Color PINK = new Color(MAX_BYTE, 175, 175);
	public static final Color ORANGE = new Color(MAX_BYTE, 200, MIN_BYTE);
	public static final Color YELLOW = new Color(MAX_BYTE, MAX_BYTE, MIN_BYTE);
	public static final Color GREEN = new Color(MIN_BYTE, MAX_BYTE, MIN_BYTE);
	public static final Color MAGENTA = new Color(MAX_BYTE, MIN_BYTE, MAX_BYTE);
	public static final Color CYAN = new Color(MIN_BYTE, MAX_BYTE, MAX_BYTE);
	public static final Color BLUE = new Color(MIN_BYTE, MIN_BYTE, MAX_BYTE);

	private static final int SHIFT_RED = 2;
	private static final int SHIFT_GREEN = 1;
	private static final int SHIFT_BLUE = 0;

	private final int value;

	public static Color decode(String nm) throws NumberFormatException {
		int value = Integer.decode(nm).intValue();
		return new Color(shiftRight(value, SHIFT_RED), shiftRight(value, SHIFT_GREEN), shiftRight(value, SHIFT_BLUE));
	}

	public Color(int red, int green, int blue) {
		value = shiftLeft(red, SHIFT_RED) | shiftLeft(green, SHIFT_GREEN) | shiftLeft(blue, SHIFT_BLUE);
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
		return (value & MAX_BYTE) << bytes * BITS_PER_BYTE;
	}

	private static int shiftRight(int value, int bytes) {
		return value >> bytes * BITS_PER_BYTE & MAX_BYTE;
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
