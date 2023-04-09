package com.github.smartfootballtable.ledcontrol.scene;

import com.github.smartfootballtable.ledcontrol.Animator;

public final class DummyAnimator implements Animator {

	private Runnable runnable;

	@Override
	public AnimatorTask start(Runnable runnable) {
		this.runnable = runnable;
		return new AnimatorTask() {
			@Override
			public void stop() {
				// nothing todo
			}
		};
	}

	public void next() {
		runnable.run();
	}

}