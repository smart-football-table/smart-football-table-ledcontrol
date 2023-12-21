package com.github.smartfootballtable.ledcontrol.panel;

import static com.github.smartfootballtable.ledcontrol.Color.BLACK;
import static com.github.smartfootballtable.ledcontrol.Color.GREEN;
import static com.github.smartfootballtable.ledcontrol.Color.RED;
import static com.github.smartfootballtable.ledcontrol.panel.Panel.OverlayStrategy.transparentOn;
import static com.github.smartfootballtable.ledcontrol.scene.FlashScene.FlashConfig.flash;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.function.ToIntFunction;

import org.junit.jupiter.api.Test;

import com.github.smartfootballtable.ledcontrol.Color;
import com.github.smartfootballtable.ledcontrol.panel.Panel.OverlayStrategy;
import com.github.smartfootballtable.ledcontrol.scene.DummyAnimator;
import com.github.smartfootballtable.ledcontrol.scene.FlashScene;

class StackedPanelTest {

	private static final Color OFF = null;

	private Color[][] colors;

	private DummyAnimator animator = new DummyAnimator();

	@Test
	void paintThrough() {
		StackedPanel sut = newSut(3, 2);
		Panel inner = sut.createSubPanel();
		inner.setColor(2, 1, RED);
		inner.repaint();
		assertThat(getColors(sut), is(new Color[][] { //
				new Color[] { OFF, OFF, OFF }, //
				new Color[] { OFF, OFF, RED } //
		}));

	}

	@Test
	void twoPanels() {
		StackedPanel sut = newSut(3, 2);
		Panel inner1 = sut.createSubPanel();
		Panel inner2 = sut.createSubPanel();
		inner1.setColor(1, 0, RED);
		inner1.repaint();

		inner2.setColor(2, 1, GREEN);
		inner2.repaint();

		assertThat(getColors(sut), is(new Color[][] { //
				{ OFF, RED, OFF }, //
				{ OFF, OFF, GREEN } //
		}));
	}

	@Test
	void reappears() {
		StackedPanel sut = newSut(3, 2);
		Panel inner1 = sut.createSubPanel();
		Panel inner2 = sut.createSubPanel();
		inner1.fill(RED);
		inner1.repaint();

		inner2.fill(GREEN);
		inner2.repaint();

		inner2.fill(OFF);
		inner2.repaint();

		assertThat(getColors(sut), is(new Color[][] { //
				{ RED, RED, RED }, //
				{ RED, RED, RED } }));
	}

	@Test
	void mixTwoColors() {
		StackedPanel sut = newSut(1, 1);
		Panel inner1 = sut.createSubPanel();
		Panel inner2 = sut.createSubPanel().overlayStrategy(mixColorsStrategy());
		setColors(inner1, RED);
		setColors(inner2, GREEN);
		assertColors(sut, new Color(127, 127, 0));
	}

	@Test
	void transparent() {
		StackedPanel sut = newSut(4, 1);
		Panel inner1 = sut.createSubPanel();
		Panel inner2 = sut.createSubPanel().overlayStrategy(transparentOn(RED));
		setColors(inner1, RED, GREEN, RED, null);
		setColors(inner2, GREEN, RED, null, RED);
		assertColors(sut, GREEN, GREEN, null, null);
	}

	private static OverlayStrategy mixColorsStrategy() {
		return new OverlayStrategy() {

			@Override
			public void copy(int x, int y, Color color, Panel target) {
				target.setColor(x, y, mix(color, target.getColors()[y][x]));
			}

			private Color mix(Color c1, Color c2) {
				if (c1 == null) {
					return c2;
				}
				if (c2 == null) {
					return c1;
				}
				List<Color> colors = asList(c1, c2);
				return new Color( //
						avg(colors, Color::getRed), //
						avg(colors, Color::getGreen), //
						avg(colors, Color::getBlue) //
				);
			}

			private int avg(List<Color> colors, ToIntFunction<Color> toIntFunction) {
				return (int) colors.stream().mapToInt(toIntFunction).average().getAsDouble();
			}

		};
	}

	@Test
	void flashOnBlack() {
		StackedPanel sut = newSut(3, 2);
		Color colorOnUnderlyingPanel = BLACK;
		sut.createSubPanel().fill(colorOnUnderlyingPanel);
		Color flashColor = GREEN;
		doFlash(new FlashScene(sut.createSubPanel(), flash(flashColor, 1)));
		animator.next();
		assertThat(getColors(sut), is(new Color[][] { //
				{ flashColor, flashColor, flashColor }, //
				{ flashColor, flashColor, flashColor } //
		}));
		animator.next();
		assertThat(getColors(sut), is(new Color[][] { //
				{ colorOnUnderlyingPanel, colorOnUnderlyingPanel, colorOnUnderlyingPanel }, //
				{ colorOnUnderlyingPanel, colorOnUnderlyingPanel, colorOnUnderlyingPanel } //
		}));
	}

	@Test
	void flashOnColor() {
		StackedPanel sut = newSut(3, 2);
		Color colorOnUnderlyingPanel = RED;
		sut.createSubPanel().fill(colorOnUnderlyingPanel);
		Color flashColor = GREEN;
		doFlash(new FlashScene(sut.createSubPanel(), flash(flashColor, 1)));
		animator.next();
		assertThat(getColors(sut), is(new Color[][] { //
				{ flashColor, flashColor, flashColor }, //
				{ flashColor, flashColor, flashColor } //
		}));
		animator.next();
		assertThat(getColors(sut), is(new Color[][] { //
				{ colorOnUnderlyingPanel, colorOnUnderlyingPanel, colorOnUnderlyingPanel }, //
				{ colorOnUnderlyingPanel, colorOnUnderlyingPanel, colorOnUnderlyingPanel } //
		}));
	}

	private void setColors(Panel panel, Color... colors) {
		for (int i = 0; i < colors.length; i++) {
			panel.setColor(i, 0, colors[i]);
		}
		panel.repaint();
	}

	private void assertColors(StackedPanel sut, Color... colors) {
		assertThat(getColors(sut), is(new Color[][] { colors }));
	}

	private void doFlash(FlashScene flashScene) {
		flashScene.flash(animator);
	}

	private StackedPanel newSut(int width, int height) {
		StackedPanel sut = new StackedPanel(width, height);
		sut.addRepaintListener(p -> colors = p.getColors());
		return sut;
	}

	private Color[][] getColors(Panel sut) {
		return colors;
	}

}
