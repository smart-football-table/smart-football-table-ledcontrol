package ledcontrol.scene;

import static java.awt.Color.BLACK;
import static java.awt.Color.GREEN;
import static java.awt.Color.RED;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.awt.Color;

import org.junit.Test;

import ledcontrol.panel.Panel;

public class GoalSceneTest {

	private Panel panel = new Panel(2 + 1 + 2, 3);
	private GoalScene sut = new GoalScene(panel, RED, GREEN);

	@Test
	public void _0_0() {
		whenScoreIs(0, 0);
		thenPanelIs(new Color[][] { //
				{ BLACK, BLACK, BLACK, BLACK, BLACK }, //
				{ BLACK, BLACK, BLACK, BLACK, BLACK }, //
				{ BLACK, BLACK, BLACK, BLACK, BLACK } //
		});
	}

	@Test
	public void _1_0() {
		whenScoreIs(1, 0);
		thenPanelIs(new Color[][] { //
				{ RED, BLACK, BLACK, BLACK, BLACK }, //
				{ RED, BLACK, BLACK, BLACK, BLACK }, //
				{ RED, BLACK, BLACK, BLACK, BLACK } //
		});
	}

	@Test
	public void _0_1() {
		whenScoreIs(0, 1);
		thenPanelIs(new Color[][] { //
				{ BLACK, BLACK, BLACK, BLACK, GREEN }, //
				{ BLACK, BLACK, BLACK, BLACK, GREEN }, //
				{ BLACK, BLACK, BLACK, BLACK, GREEN } //
		});
	}

	@Test
	public void _2_2() {
		whenScoreIs(2, 2);
		thenPanelIs(new Color[][] { //
				{ RED, RED, BLACK, GREEN, GREEN }, //
				{ RED, RED, BLACK, GREEN, GREEN }, //
				{ RED, RED, BLACK, GREEN, GREEN } //
		});
	}

	private void thenPanelIs(Color[][] value) {
		assertThat(panel.getColors(), is(value));
	}

	private void whenScoreIs(int a, int b) {
		sut.setScore(a, b);
	}

}
