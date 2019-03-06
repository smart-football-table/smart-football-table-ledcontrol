package ledcontrol.scene;

import static java.awt.Color.BLACK;
import static java.awt.Color.BLUE;
import static java.awt.Color.RED;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.awt.Color;

import org.junit.Test;

import ledcontrol.panel.Panel;

public class IdleSceneTest {

	private DummyAnimator animator = new DummyAnimator();
	private Panel panel;
	private IdleScene sut;

	@Test
	public void startOfAnimation() {
		sutOnPanel(3, 2);
		animationIsStarted();
		thenPanelIs(new Color[][] { //
				{ null, null, null, }, //
				{ null, null, null, }, //
		});

		afterSwitchPanelIs(new Color[][] { //
				{ BLACK, null, null, }, //
				{ BLACK, null, null, }, //
		});
		afterSwitchPanelIs(new Color[][] { //
				{ BLACK, BLACK, null, }, //
				{ BLACK, BLACK, null, }, //
		});
		afterSwitchPanelIs(new Color[][] { //
				{ BLACK, BLACK, BLACK, }, //
				{ BLACK, BLACK, BLACK, }, //
		});

		afterSwitchPanelIs(new Color[][] { //
				{ BLUE, BLACK, BLACK, }, //
				{ BLUE, BLACK, BLACK, }, //
		});
		afterSwitchPanelIs(new Color[][] { //
				{ BLUE, BLUE, BLACK, }, //
				{ BLUE, BLUE, BLACK, }, //
		});
		afterSwitchPanelIs(new Color[][] { //
				{ BLUE, BLUE, BLUE, }, //
				{ BLUE, BLUE, BLUE, }, //
		});
		afterSwitchPanelIs(new Color[][] { //
				{ RED, BLUE, BLUE, }, //
				{ RED, BLUE, BLUE, }, //
		});
		afterSwitchPanelIs(new Color[][] { //
				{ RED, RED, BLUE, }, //
				{ RED, RED, BLUE, }, //
		});
		afterSwitchPanelIs(new Color[][] { //
				{ RED, RED, RED, }, //
				{ RED, RED, RED, }, //
		});
		afterSwitchPanelIs(new Color[][] { //
				{ BLACK, RED, RED, }, //
				{ BLACK, RED, RED, }, //
		});
		afterSwitchPanelIs(new Color[][] { //
				{ BLACK, BLACK, RED, }, //
				{ BLACK, BLACK, RED, }, //
		});
		// ------------------------------------------------------------
		afterSwitchPanelIs(new Color[][] { //
				{ BLACK, BLACK, BLACK, }, //
				{ BLACK, BLACK, BLACK, }, //
		});
		afterSwitchPanelIs(new Color[][] { //
				{ BLUE, BLACK, BLACK, }, //
				{ BLUE, BLACK, BLACK, }, //
		});

	}

	private void afterSwitchPanelIs(Color[][] colors) {
		animator.next();
		thenPanelIs(colors);
	}

	private IdleScene sutOnPanel(int columns, int rows) {
		panel = new Panel(columns, rows);
		sut = new IdleScene(panel, BLACK, BLUE, RED);
		return sut;
	}

	private void thenPanelIs(Color[][] value) {
		assertThat(panel.getColors(), is(value));
	}

	private void animationIsStarted() {
		sut.startAnimation(animator);
	}

}
