package ledcontrol.scene;

import java.awt.Color;

import ledcontrol.panel.Panel;

public class GoalScene implements Scene {

	private final Panel panel;
	private final Color[] colors;

	public GoalScene(Panel panel, Color... colors) {
		this.panel = panel;
		this.colors = colors;
	}

	public void setScore(int... scores) {
		repaint(scores);
	}

	private void repaint(int... scores) {
		for (int x = 0; x < panel.getWidth(); x++) {
			if (scores[0] > x) {
				drawColumn(x, colors[0]);
			}
			if (x >= panel.getWidth() - scores[1]) {
				drawColumn(x, colors[1]);
			}
		}
		panel.repaint();
	}

	private void drawColumn(int col, Color color) {
		for (int y = 0; y < panel.getHeight(); y++) {
			panel.setColor(col, y, color);
		}
	}

}