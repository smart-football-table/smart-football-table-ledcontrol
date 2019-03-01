package ledcontrol.scene;

import static java.awt.Color.BLUE;
import static java.awt.Color.RED;

import java.awt.Color;

import ledcontrol.TheSystem.Animator;
import ledcontrol.panel.Panel;

public class IdleScene implements Scene {

	private int x = 0;
	private final Panel panel;

	public IdleScene(Panel panel) {
		this.panel = panel;
	}

	public void startAnimation(Animator animator) {
		animator.start(this::nextStep);
	}

	private void nextStep() {
		int width = panel.getWidth();
		drawColumn(x, BLUE);
		drawColumn(width - 1 - x, RED);
		panel.repaint();
		x++;
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