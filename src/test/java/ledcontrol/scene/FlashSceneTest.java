package ledcontrol.scene;

import static java.awt.Color.BLACK;
import static java.awt.Color.RED;
import static java.awt.Color.YELLOW;
import static java.util.stream.IntStream.range;
import static ledcontrol.scene.FlashScene.FlashConfig.flash;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.awt.Color;

import org.junit.jupiter.api.Test;

import ledcontrol.panel.Panel;

class FlashSceneTest {

	private DummyAnimator animator = new DummyAnimator();

	private static final Color ____ = null;

	private Panel panel;
	private FlashScene sut;

	@Test
	void flashYellowBlackRed() {
		sutOnPanel(3, 2);
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
		Color[] row = range(0, panel.getWidth()).mapToObj(x -> color).toArray(Color[]::new);
		return range(0, panel.getHeight()).mapToObj(y -> row).toArray(Color[][]::new);
	}

	private FlashScene sutOnPanel(int columns, int rows) {
		panel = new Panel(columns, rows);
		sut = new FlashScene(panel, flash(YELLOW, 2), flash(BLACK, 1), flash(RED, 3));
		return sut;
	}

	private void thenPanelIs(Color[][] value) {
		assertThat(panel.getColors(), is(value));
	}

	private void afterSwitchPanelIs(Color[][] colors) {
		animator.next();
		thenPanelIs(colors);
	}

	private void whenFlash() {
		sut.flash(animator);
	}

}
