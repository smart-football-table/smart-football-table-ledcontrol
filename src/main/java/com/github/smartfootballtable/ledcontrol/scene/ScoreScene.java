package com.github.smartfootballtable.ledcontrol.scene;

import static java.util.function.IntUnaryOperator.identity;

import java.util.function.IntUnaryOperator;

import com.github.smartfootballtable.ledcontrol.Color;
import com.github.smartfootballtable.ledcontrol.panel.Panel;

public class ScoreScene implements Scene {

	private final Panel panel;
	private final Color[] colors;
	private final IntUnaryOperator xNormal = identity();
	private final IntUnaryOperator xReversed;
	private int pixelsPerGoal = 1;
	private int spaceDots;

	public ScoreScene(Panel panel, Color... colors) {
		this.panel = panel;
		this.colors = colors;
		this.xReversed = x -> panel.getWidth() - x - 1;
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
			draw(teamid, score, xNormal);
		}
		if (teamid == 1) {
			draw(teamid, score, xReversed);
		}
		panel.repaint();
	}

	private void draw(int teamid, int score, IntUnaryOperator xCalculaor) {
		for (int x = 0; x < panel.getWidth() / 2; x++) {
			drawColumn(xCalculaor.applyAsInt(x), isGoalDot(x, score) ? colors[teamid] : null);
		}
	}

	private boolean isGoalDot(int x, int score) {
		return isGoalArea(x, score) && !isSpaceDot(x);
	}

	private boolean isGoalArea(int x, int score) {
		return x < score * (pixelsPerGoal + spaceDots);
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