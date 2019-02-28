package ledcontrol.panel;

import static java.awt.Color.BLACK;
import static java.awt.Color.GREEN;
import static java.awt.Color.RED;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.awt.Color;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import ledcontrol.panel.MixPanel.OverlayStrategy;
import ledcontrol.scene.FlashScene;

public class MixPanelTest {

	private static final Color OFF = null;

	private Color[][] colors;

	@Test
	public void writeThrough() {
		MixPanel sut = newSut(3, 2);
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
		MixPanel sut = newSut(3, 2);
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
		MixPanel sut = newSut(3, 2);
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
		MixPanel sut = newSut(1, 1).setOverlayStrategy(new OverlayStrategy() {
			@Override
			public void copy(int x, int y, Color color, Panel target) {
				Color c1 = orBlack(color);
				Color c2 = orBlack(target.getColors()[y][x]);
				int r = (c1.getRed() + c2.getRed()) / 2;
				int g = (c1.getGreen() + c2.getGreen()) / 2;
				int b = (c1.getBlue() + c2.getBlue()) / 2;
				target.setColor(x, y, new Color(r, g, b));
			}

			private Color orBlack(Color color) {
				return color == null ? BLACK : color;
			}
		});

		Panel inner1 = sut.createSubPanel();
		Panel inner2 = sut.createSubPanel();
		inner1.setColor(0, 0, RED);
		inner2.setColor(0, 0, GREEN);
		inner1.repaint();
		inner2.repaint();
		// TODO are this the right colors
		assertThat(getColors(sut), is(new Color[][] { //
				new Color[] { new Color(79, 159, 0) } //
		}));
	}

	@Test
	// TODO Peter ...continue here :-) (professional correct, implementation not)
	public void flashOnBlack() throws InterruptedException {
		MixPanel sut = newSut(3, 2);
		Color colorOnUnderlyingPanel = BLACK;
		sut.createSubPanel().fill(colorOnUnderlyingPanel);
		Color flashColor = GREEN;
		new FlashScene(sut.createSubPanel()) {
			@Override
			protected void sleep(TimeUnit unit, long duration) throws InterruptedException {
				assertThat(getColors(sut), is(new Color[][] { //
						{ flashColor, flashColor, flashColor }, //
						{ flashColor, flashColor, flashColor } //
				}));
				super.sleep(unit, duration);
			}
		}.flash(flashColor, TimeUnit.MILLISECONDS, 10);
		TimeUnit.MILLISECONDS.sleep(100);
		assertThat(getColors(sut), is(new Color[][] { //
				{ colorOnUnderlyingPanel, colorOnUnderlyingPanel, colorOnUnderlyingPanel }, //
				{ colorOnUnderlyingPanel, colorOnUnderlyingPanel, colorOnUnderlyingPanel } //
		}));
	}

	@Test
	public void flashOnColor() throws InterruptedException {
		MixPanel sut = newSut(3, 2);
		Color colorOnUnderlyingPanel = RED;
		sut.createSubPanel().fill(colorOnUnderlyingPanel);
		Color flashColor = GREEN;
		new FlashScene(sut.createSubPanel()) {
			@Override
			protected void sleep(TimeUnit unit, long duration) throws InterruptedException {
				assertThat(getColors(sut), is(new Color[][] { //
						{ flashColor, flashColor, flashColor }, //
						{ flashColor, flashColor, flashColor } //
				}));
				super.sleep(unit, duration);
			}
		}.flash(flashColor, TimeUnit.MILLISECONDS, 10);
		TimeUnit.MILLISECONDS.sleep(100);
		assertThat(getColors(sut), is(new Color[][] { //
				{ colorOnUnderlyingPanel, colorOnUnderlyingPanel, colorOnUnderlyingPanel }, //
				{ colorOnUnderlyingPanel, colorOnUnderlyingPanel, colorOnUnderlyingPanel } //
		}));
	}

	private MixPanel newSut(int width, int height) {
		MixPanel sut = new MixPanel(width, height);
		sut.addRepaintListener(p -> colors = p.getColors());
		return sut;
	}

	private Color[][] getColors(Panel sut) {
		return colors;
	}

}
