package com.github.smartfootballtable.ledcontrol.util;

public final class Preconditions {

	private Preconditions() {
		super();
	}

	public static void checkState(boolean b, String message, Object... args) {
		if (!b) {
			throw new IllegalStateException(String.format(message, args));
		}
	}

}