package ledcontrol.scene;

import static java.awt.Color.GREEN;
import static java.awt.Color.RED;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.awt.Color;

import org.junit.jupiter.api.Test;

import ledcontrol.panel.Panel;

class ScoreSceneTest {

	private static final Color ____ = null;

	private Panel panel;
	private ScoreScene sut;

	@Test
	void _0_0() {
		sutOnPanel(2 + 1 + 2, 3);
		whenScoreIs(0, 0);
		thenPanelIs(new Color[][] { //
				{ ____, ____, ____, ____, ____ }, //
				{ ____, ____, ____, ____, ____ }, //
				{ ____, ____, ____, ____, ____ } //
		});
	}

	@Test
	void _1_0() {
		sutOnPanel(2 + 1 + 2, 3);
		whenScoreIs(1, 0);
		thenPanelIs(new Color[][] { //
				{ RED, ____, ____, ____, ____ }, //
				{ RED, ____, ____, ____, ____ }, //
				{ RED, ____, ____, ____, ____ }, //
		});
	}

	@Test
	void _0_1() {
		sutOnPanel(2 + 1 + 2, 3);
		whenScoreIs(0, 1);
		thenPanelIs(new Color[][] { //
				{ ____, ____, ____, ____, GREEN }, //
				{ ____, ____, ____, ____, GREEN }, //
				{ ____, ____, ____, ____, GREEN }, //
		});
	}

	@Test
	void _2_2() {
		sutOnPanel(2 + 1 + 2, 3);
		whenScoreIs(2, 2);
		thenPanelIs(new Color[][] { //
				{ RED, RED, ____, GREEN, GREEN }, //
				{ RED, RED, ____, GREEN, GREEN }, //
				{ RED, RED, ____, GREEN, GREEN }, //
		});
	}

	@Test
	void centerWillNotBePainted() {
		sutOnPanel(2 + 1 + 2, 3);
		whenScoreIs(3, 3);
		thenPanelIs(new Color[][] { //
				{ RED, RED, ____, GREEN, GREEN }, //
				{ RED, RED, ____, GREEN, GREEN }, //
				{ RED, RED, ____, GREEN, GREEN }, //
		});
	}

	@Test
	void afterResettingTheScoresThePanelIsBlank() {
		sutOnPanel(2 + 1 + 2, 3);
		whenScoreIs(3, 3);
		whenScoreIs(0, 0);
		thenPanelIs(new Color[][] { //
				{ ____, ____, ____, ____, ____ }, //
				{ ____, ____, ____, ____, ____ }, //
				{ ____, ____, ____, ____, ____ }, //
		});
	}

	@Test
	void _2_1_with_2_leds_per_goal() {
		sutOnPanel(4 + 1 + 4, 5).pixelsPerGoal(2);
		whenScoreIs(2, 1);
		thenPanelIs(new Color[][] { //
				{ RED, RED, RED, RED, ____, ____, ____, GREEN, GREEN }, //
				{ RED, RED, RED, RED, ____, ____, ____, GREEN, GREEN }, //
				{ RED, RED, RED, RED, ____, ____, ____, GREEN, GREEN }, //
				{ RED, RED, RED, RED, ____, ____, ____, GREEN, GREEN }, //
				{ RED, RED, RED, RED, ____, ____, ____, GREEN, GREEN }, //
		});
	}

	@Test
	void _2_1_with_2_leds_per_goal_with_space_dots() {
		sutOnPanel(7 + 1 + 7, 2).pixelsPerGoal(2).spaceDots(3);
		whenScoreIs(2, 1);
		thenPanelIs(new Color[][] { //
				{ RED, RED, ____, ____, ____, RED, RED, ____, ____, ____, ____, ____, ____, GREEN, GREEN }, //
				{ RED, RED, ____, ____, ____, RED, RED, ____, ____, ____, ____, ____, ____, GREEN, GREEN }, //
		});
	}

	private ScoreScene sutOnPanel(int columns, int rows) {
		panel = new Panel(columns, rows);
		sut = new ScoreScene(panel, RED, GREEN);
		return sut;
	}

	private void thenPanelIs(Color[][] value) {
		assertThat(panel.getColors(), is(value));
	}

	private void whenScoreIs(int scoreTeam0, int scoreTeam1) {
		sut.setScore(0, scoreTeam0);
		sut.setScore(1, scoreTeam1);
	}

}
