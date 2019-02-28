package ledcontrol.panel;

import static java.awt.Color.BLACK;
import static java.awt.Color.GREEN;
import static java.awt.Color.RED;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.awt.Color;

import org.junit.Test;

import ledcontrol.panel.StackedPanel.OverlayStrategy;
import ledcontrol.scene.FlashScene;

public class StackedPanelTest {

	private static final Color OFF = null;

	private Color[][] colors;

	@Test
	public void writeThrough() {
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
	public void twoPanels() {
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
	public void reappears() {
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
	public void mixTwoColors() {
		StackedPanel sut = newSut(1, 1).setOverlayStrategy(new OverlayStrategy() {
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
				int r = (c1.getRed() + c2.getRed()) / 2;
				int g = (c1.getGreen() + c2.getGreen()) / 2;
				int b = (c1.getBlue() + c2.getBlue()) / 2;
				return new Color(r, g, b);
			}
		});

		Panel inner1 = sut.createSubPanel();
		Panel inner2 = sut.createSubPanel();
		inner1.setColor(0, 0, RED);
		inner1.repaint();
		inner2.setColor(0, 0, GREEN);
		inner2.repaint();
		assertThat(getColors(sut), is(new Color[][] { //
				new Color[] { new Color(127, 127, 0) } //
		}));
	}

	@Test
	public void flashOnBlack() {
		StackedPanel sut = newSut(3, 2);
		Color colorOnUnderlyingPanel = BLACK;
		sut.createSubPanel().fill(colorOnUnderlyingPanel);
		Color flashColor = GREEN;
		FlashScene flashScene = new FlashScene(sut.createSubPanel());
		flashScene.fill(flashColor);
		flashScene.clear();
		assertThat(getColors(sut), is(new Color[][] { //
				{ colorOnUnderlyingPanel, colorOnUnderlyingPanel, colorOnUnderlyingPanel }, //
				{ colorOnUnderlyingPanel, colorOnUnderlyingPanel, colorOnUnderlyingPanel } //
		}));
	}

	@Test
	public void flashOnColor() {
		StackedPanel sut = newSut(3, 2);
		Color colorOnUnderlyingPanel = RED;
		sut.createSubPanel().fill(colorOnUnderlyingPanel);
		Color flashColor = GREEN;
		FlashScene flashScene = new FlashScene(sut.createSubPanel());
		flashScene.fill(flashColor);
		flashScene.clear();
		assertThat(getColors(sut), is(new Color[][] { //
				{ colorOnUnderlyingPanel, colorOnUnderlyingPanel, colorOnUnderlyingPanel }, //
				{ colorOnUnderlyingPanel, colorOnUnderlyingPanel, colorOnUnderlyingPanel } //
		}));
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
