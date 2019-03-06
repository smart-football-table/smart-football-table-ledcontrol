package ledcontrol.scene;

import java.awt.Color;

import ledcontrol.Animator;
import ledcontrol.Animator.AnimatorTask;
import ledcontrol.panel.Panel;

public class FlashScene implements Scene {

	public static class FlashConfig {

		public static FlashConfig flash(Color color, int ticks) {
			return new FlashConfig(color, ticks);
		}

		private final Color color;
		private final int ticks;

		private FlashConfig(Color color, int ticks) {
			this.color = color;
			this.ticks = ticks;
		}

	}

	private final Panel panel;
	private int currentConfig = -1;
	private FlashConfig[] flashConfigs;

	private AnimatorTask task;
	private int tick;

	public FlashScene(Panel panel, FlashConfig... flashConfigs) {
		this.panel = panel;
		this.flashConfigs = flashConfigs.clone();
	}

	public void flash(Animator animator) {
		task = animator.start(this::tick);
	}

	private void tick() {
		if (tick == 0) {
			if (++currentConfig == flashConfigs.length) {
				panel.clear();
				panel.repaint();
				this.task.stop();
				return;
			}
			panel.fill(flashConfigs[currentConfig].color);
			panel.repaint();
		}
		if (++tick == flashConfigs[currentConfig].ticks) {
			tick = 0;
		}

	}

}