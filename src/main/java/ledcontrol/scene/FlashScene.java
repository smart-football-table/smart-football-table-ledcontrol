package ledcontrol.scene;

import java.awt.Color;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import ledcontrol.panel.Panel;

public class FlashScene implements Scene {

	private final Panel panel;

	public FlashScene(Panel panel) {
		this.panel = panel;
	}

	public void flash(Color color, TimeUnit unit, long duration) {
		panel.fill(color);
		panel.repaint();
		Executors.newSingleThreadExecutor().submit(() -> {
			sleep(unit, duration);
			panel.clear();
			panel.repaint();
			return null;
		});
	}

	protected void sleep(TimeUnit unit, long duration) throws InterruptedException {
		unit.sleep(duration);
	}

}