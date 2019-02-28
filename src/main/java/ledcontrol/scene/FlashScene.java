package ledcontrol.scene;

import java.awt.Color;

import ledcontrol.panel.Panel;

public class FlashScene implements Scene {

	private final Panel panel;

	public FlashScene(Panel panel) {
		this.panel = panel;
	}

	public void fill(Color color) {
		panel.fill(color);
		panel.repaint();
	}

	public void clear() {
		panel.clear();
		panel.repaint();
	}

}