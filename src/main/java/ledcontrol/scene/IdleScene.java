package ledcontrol.scene;

import java.awt.Color;

import ledcontrol.Animator;
import ledcontrol.Animator.AnimatorTask;
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
	private AnimatorTask task;
	private Color[] paintColors;

	public IdleScene(Panel panel, Color... paintColors) {
		this.panel = panel;
		this.paintColors = paintColors.clone();
	}

	public synchronized void startAnimation(Animator animator) {
		if (task == null) {
			panel.clear();
			panel.repaint();
			task = animator.start(this::nextStep);
		}
	}

	public synchronized IdleScene stopAnimation() {
		task.stop();
		task = null;
		panel.clear();
		panel.repaint();
		return this;
	}

	public IdleScene reset() {
		x = 0;
		currentColorIdx = 0;
		return this;
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

}