package ledcontrol.scene;

import static java.awt.Color.GREEN;
import static java.awt.Color.RED;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.awt.Color;

import org.junit.Test;

import ledcontrol.panel.Panel;

public class GoalSceneTest {

	private static final Color OFF = null;

	private Panel panel = new Panel(2 + 1 + 2, 3);
	private GoalScene sut = new GoalScene(panel, RED, GREEN);

	@Test
	public void _0_0() {
		whenScoreIs(0, 0);
		thenPanelIs(new Color[][] { //
				{ OFF, OFF, OFF, OFF, OFF }, //
				{ OFF, OFF, OFF, OFF, OFF }, //
				{ OFF, OFF, OFF, OFF, OFF } //
		});
	}

	@Test
	public void _1_0() {
		whenScoreIs(1, 0);
		thenPanelIs(new Color[][] { //
				{ RED, OFF, OFF, OFF, OFF }, //
				{ RED, OFF, OFF, OFF, OFF }, //
				{ RED, OFF, OFF, OFF, OFF } //
		});
	}

	@Test
	public void _0_1() {
		whenScoreIs(0, 1);
		thenPanelIs(new Color[][] { //
				{ OFF, OFF, OFF, OFF, GREEN }, //
				{ OFF, OFF, OFF, OFF, GREEN }, //
				{ OFF, OFF, OFF, OFF, GREEN } //
		});
	}

	@Test
	public void _2_2() {
		whenScoreIs(2, 2);
		thenPanelIs(new Color[][] { //
				{ RED, RED, OFF, GREEN, GREEN }, //
				{ RED, RED, OFF, GREEN, GREEN }, //
				{ RED, RED, OFF, GREEN, GREEN } //
		});
	}

	private void thenPanelIs(Color[][] value) {
		assertThat(panel.getColors(), is(value));
	}

	private void whenScoreIs(int a, int b) {
		sut.setScore(a, b);
	}

}
