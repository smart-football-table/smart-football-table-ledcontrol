package com.github.smartfootballtable.ledcontrol.scene;

import static com.github.smartfootballtable.ledcontrol.Color.BLACK;
import static com.github.smartfootballtable.ledcontrol.Color.RED;
import static com.github.smartfootballtable.ledcontrol.Color.YELLOW;
import static com.github.smartfootballtable.ledcontrol.scene.FlashScene.FlashConfig.flash;
import static java.util.stream.Stream.generate;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.jupiter.api.Test;

import com.github.smartfootballtable.ledcontrol.Color;
import com.github.smartfootballtable.ledcontrol.panel.Panel;

class FlashSceneTest {

	private DummyAnimator animator = new DummyAnimator();

	private static final Color ____ = null;

	private Panel panel;
	private FlashScene sut;

	@Test
	void flashYellowBlackRed() {
		sutOnPanelOfSize(3, 2);
		whenFlash();
		afterSwitchPanelIs(allOf(YELLOW));
		afterSwitchPanelIs(allOf(YELLOW));
		afterSwitchPanelIs(allOf(BLACK));
		afterSwitchPanelIs(allOf(RED));
		afterSwitchPanelIs(allOf(RED));
		afterSwitchPanelIs(allOf(RED));
		afterSwitchPanelIs(allOf(____));
	}

	private Color[][] allOf(Color color) {
		return generate(() -> row(color)).limit(panel.getHeight()).toArray(Color[][]::new);
	}

	private Color[] row(Color color) {
		return generate(() -> color).limit(panel.getWidth()).toArray(Color[]::new);
	}

	private void sutOnPanelOfSize(int columns, int rows) {
		panel = new Panel(columns, rows);
		sut = new FlashScene(panel, flash(YELLOW, 2), flash(BLACK, 1), flash(RED, 3));
	}

	private void afterSwitchPanelIs(Color[][] colors) {
		animator.next();
		thenPanelIs(colors);
	}

	private void thenPanelIs(Color[][] value) {
		assertThat(panel.getColors(), is(value));
	}
	
	private void whenFlash() {
		sut.flash(animator);
	}

}
