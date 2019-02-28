package ledcontrol.scene;

import static java.lang.Math.min;

import java.awt.Color;

import ledcontrol.panel.Panel;

public class ScoreScene implements Scene {

	private final Panel panel;
	private final Color[] colors;

	public ScoreScene(Panel panel, Color... colors) {
		this.panel = panel;
		this.colors = colors;
	}

	public void setScore(int... scores) {
		repaint(scores);
	}

	private void repaint(int... scores) {
		panel.clear();
		int width = panel.getWidth();
		for (int x = 0; x < min(scores[0], width / 2); x++) {
			drawColumn(x, colors[0]);
		}
		for (int x = 0; x < min(scores[1], width / 2); x++) {
			drawColumn(width - x - 1, colors[1]);
		}
		panel.repaint();
	}

	private void drawColumn(int col, Color color) {
		for (int y = 0; y < panel.getHeight(); y++) {
			panel.setColor(col, y, color);
		}
	}

}