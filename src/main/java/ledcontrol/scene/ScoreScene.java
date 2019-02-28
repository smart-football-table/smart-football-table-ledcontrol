package ledcontrol.scene;

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
		// TODO maximum half of width
		for (int x = 0; x < scores[0]; x++) {
			drawColumn(x, colors[0]);
		}
		// TODO maximum half of width
		for (int x = 0; x < scores[1]; x++) {
			drawColumn(panel.getWidth() - x - 1, colors[1]);
		}
		panel.repaint();
	}

	private void drawColumn(int col, Color color) {
		for (int y = 0; y < panel.getHeight(); y++) {
			panel.setColor(col, y, color);
		}
	}

}