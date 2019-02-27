package ledcontrol.scene;

import java.awt.Color;

import ledcontrol.panel.Panel;

public class GoalScene implements Scene {

	private final Color color;
	private int score = 0;

	private Panel panel;

	public GoalScene(Panel panel, Color color) {
		this.panel = panel;
		this.color = color;
	}

	public void incrementScore() {
		score++;
		for (int x = 0; x < panel.getWidth(); x++) {
			if (score > x) {
				for (int y = 0; y < panel.getHeight(); y++) {
					panel.setColor(x, y, color);
				}
			}
		}
		panel.repaint();
	}

}