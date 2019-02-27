package ledcontrol.scene;

import static java.awt.Color.BLACK;

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
			panel.fill(BLACK);
			panel.repaint();
			return null;
		});
	}

	protected void sleep(TimeUnit unit, long duration) throws InterruptedException {
		unit.sleep(duration);
	}

}