package com.github.smartfootballtable.ledcontrol;

import static java.util.Arrays.asList;

import java.util.List;

public enum ColorFormat {

	RGB(3) {
		int writeTo(byte[] target, int pos, Color color) {
			target[pos++] = (byte) color.getRed();
			target[pos++] = (byte) color.getGreen();
			target[pos++] = (byte) color.getBlue();
			return bytesPerColor;
		}
	};

	protected final int bytesPerColor;

	private ColorFormat(int bytesPerColor) {
		this.bytesPerColor = bytesPerColor;
	}

	abstract int writeTo(byte[] target, int pos, Color color);

	public byte[] toBytes(Color[] colors) {
		return toBytes(colors, 0, colors.length);
	}

	public byte[] toBytes(Color[] colors, int offset, int length) {
		return toBytes(asList(colors), offset, length);
	}

	public byte[] toBytes(List<Color> colors) {
		return toBytes(colors, 0, colors.size());
	}

	public byte[] toBytes(List<Color> colors, int offset, int length) {
		byte[] bytes = new byte[length * bytesPerColor];
		int idx = 0;
		for (Color color : colors.subList(offset, offset + length)) {
			idx += writeTo(bytes, idx, color);
		}
		return bytes;
	}

}