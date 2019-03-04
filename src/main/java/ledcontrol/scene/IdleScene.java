package ledcontrol.scene;

import static java.awt.Color.BLACK;
import static java.awt.Color.BLUE;
import static java.awt.Color.RED;

import java.awt.Color;

import ledcontrol.Animator;
import ledcontrol.panel.Panel;

/**
 * Will start on a black screen, then paint from left to right in Color #1.
 * After then paint from left to right in Color #2. After then paint black again
 * from left to right.
 *
 */
public class IdleScene implements Scene {

	private int x = 0;
	private final Panel panel;

	private int currentColorIdx = 0;
	private final Color[] paintColors = new Color[] { BLUE, RED, BLACK };

	public IdleScene(Panel panel) {
		this.panel = panel;
	}

	public void startAnimation(Animator animator) {
		// TODO what will happen when start is called and IdleScene is already running?
		panel.fill(BLACK);
		panel.repaint();
		animator.start(this::nextStep);
	}

	private void nextStep() {
		drawColumn(x, paintColors[currentColorIdx]);
		panel.repaint();
		if (++x == panel.getWidth()) {
			if (++currentColorIdx == paintColors.length) {
				currentColorIdx = 0;
			}
			x = 0;
		}
	}

	private void drawColumn(int x, Color color) {
		int height = panel.getHeight();
		for (int y = 0; y < height; y++) {
			panel.setColor(x, y, color);
		}
	}

	public void stopAnimation() {
		// TODO Auto-generated method stub

	}

}