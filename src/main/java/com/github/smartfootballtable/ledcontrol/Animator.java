package com.github.smartfootballtable.ledcontrol;

public interface Animator {

	public interface AnimatorTask {
		void stop();
	}

	AnimatorTask start(Runnable runnable);

	void shutdown();

}