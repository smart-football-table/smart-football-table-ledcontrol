package com.github.smartfootballtable.ledcontrol;

import static com.github.smartfootballtable.ledcontrol.ColorFormat.RGB;
import static java.util.Arrays.asList;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.stream.IntStream;

public class Proto implements Closeable {

	private static final byte TPM2_HEADER_IDENT = (byte) 0x9C;
	private static final byte TPM2_DATA_FRAME = (byte) 0xDA;
	private static final byte TPM2_FOOTER_IDENT = 0x36;

	private final OutputStream outputStream;
	private final int[] frameSizes;
	private ColorFormat colorFormat = RGB;

	public static Proto forFrameSizes(OutputStream os, int... frameSizes) {
		return new Proto(os, frameSizes);
	}

	private Proto(OutputStream os, int... frameSizes) {
		this.outputStream = new BufferedOutputStream(os, sumOf(frameSizes) + 8);
		this.frameSizes = frameSizes.clone();
	}

	private int sumOf(int[] frameSizes) {
		return IntStream.of(frameSizes).sum();
	}

	public void write(Color[] colors) throws IOException {
		write(asList(colors));
	}

	public void write(List<Color> colors) throws IOException {
		int offset = 0;
		for (int currentPacket = 0; currentPacket < frameSizes.length; currentPacket++) {
			int ledsInThisFrame = frameSizes[currentPacket];
			byte[] bytes = colorFormat.toBytes(colors, offset, ledsInThisFrame);
			offset += ledsInThisFrame;
			outputStream.write(TPM2_HEADER_IDENT);
			outputStream.write(TPM2_DATA_FRAME);
			outputStream.write((bytes.length >> 8) & 0xFF);
			outputStream.write((bytes.length >> 0) & 0xFF);
			outputStream.write(currentPacket);
			outputStream.write(frameSizes.length);
			outputStream.write(bytes);
			outputStream.write(TPM2_FOOTER_IDENT);
			outputStream.flush();
		}
	}

	@Override
	public void close() throws IOException {
		outputStream.close();
	}

}
