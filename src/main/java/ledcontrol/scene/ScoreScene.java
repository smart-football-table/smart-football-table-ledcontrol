package ledcontrol.scene;

import static java.lang.Math.min;

import java.awt.Color;

import ledcontrol.panel.Panel;

public class ScoreScene implements Scene {

	private final Panel panel;
	private final Color[] colors;
	private int pixelsPerGoal = 1;
	private int spaceDots;

	public ScoreScene(Panel panel, Color... colors) {
		this.panel = panel;
		this.colors = colors;
	}

	public ScoreScene pixelsPerGoal(int pixelsPerGoal) {
		this.pixelsPerGoal = pixelsPerGoal;
		return this;
	}

	public ScoreScene spaceDots(int spaceDots) {
		this.spaceDots = spaceDots;
		return this;
	}

	public void setScore(int teamid, int score) {
		if (teamid == 0) {
			firstHalf(teamid, score);
		}
		if (teamid == 1) {
			secondHalf(teamid, score);
		}
	}

	private void firstHalf(int teamid, int score) {
		int width = panel.getWidth();
		for (int x = 0; x < width / 2; x++) {
			drawColumn(x, null);
		}
		for (int x = 0; x < min(score * (pixelsPerGoal + spaceDots), width / 2); x++) {
			if (!isSpaceDot(x)) {
				drawColumn(x, colors[teamid]);
			}
		}
		panel.repaint();
	}

	private void secondHalf(int teamid, int score) {
		int width = panel.getWidth();
		for (int x = 0; x < width / 2; x++) {
			drawColumn(width - x - 1, null);
		}
		for (int x = 0; x < min(score * (pixelsPerGoal + spaceDots), width / 2); x++) {
			if (!isSpaceDot(x)) {
				drawColumn(width - x - 1, colors[teamid]);
			}
		}
		panel.repaint();
	}

	private boolean isSpaceDot(int x) {
		return x % (pixelsPerGoal + spaceDots) >= pixelsPerGoal;
	}

	private void drawColumn(int col, Color color) {
		for (int y = 0; y < panel.getHeight(); y++) {
			panel.setColor(col, y, color);
		}
	}

}