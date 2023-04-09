package com.github.smartfootballtable.ledcontrol.panel;

import java.util.ArrayList;
import java.util.List;

public class StackedPanel extends Panel {

	private final List<Panel> inners = new ArrayList<>();
	private final RepaintListener subPanelRepaintListener;

	public StackedPanel(int width, int height) {
		super(width, height);
		this.subPanelRepaintListener = panel -> copyTo(this);
	}

	@Override
	public void copyTo(Panel target) {
		clear();
		for (Panel inner : inners) {
			inner.copyTo(target);
		}
		repaint();
	}

	public Panel createSubPanel() {
		Panel sub = new Panel(getWidth(), getHeight()).addRepaintListener(subPanelRepaintListener);
		this.inners.add(sub);
		return sub;
	}

}