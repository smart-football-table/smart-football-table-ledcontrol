package ledcontrol.panel;

import static java.awt.Color.BLACK;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class MixPanel extends Panel {

	public interface OverlayStrategy {
		OverlayStrategy DEFAULT = new OverlayStrategy() {
			private Color transparent = BLACK;

			@Override
			public void copy(int x, int y, Color newColor, Panel target) {
				if (!Objects.equals(transparent, newColor)) {
					target.setColor(x, y, newColor);
				}
			}

		};

		void copy(int x, int y, Color color, Panel target);
	}

	private final List<Panel> inners = new ArrayList<>();
	private final RepaintListener subPanelRepaintListener;

	private final List<RepaintListener> repaintListeners = new CopyOnWriteArrayList<>();
	private OverlayStrategy overlayStrategy = OverlayStrategy.DEFAULT;

	public MixPanel(int width, int height) {
		super(width, height);
		this.subPanelRepaintListener = panel -> mixTo(this);
	}

	@Override
	public void addRepaintListener(RepaintListener listener) {
		this.repaintListeners.add(listener);
	}

	public MixPanel setOverlayStrategy(OverlayStrategy overlayStrategy) {
		this.overlayStrategy = overlayStrategy;
		return this;
	}

	private void mixTo(Panel target) {
		for (Panel inner : inners) {
			Color[][] colors = inner.getColors();
			for (int y = 0; y < colors.length; y++) {
				for (int x = 0; x < colors[y].length; x++) {
					overlayStrategy.copy(x, y, colors[y][x], target);
				}
			}
		}
		repaint();
	}

	@Override
	public Panel repaint() {
		for (RepaintListener listener : repaintListeners) {
			listener.repaint(this);
		}
		return this;
	}

	public Panel createSubPanel() {
		Panel sub = new Panel(getWidth(), getHeight());
		sub.addRepaintListener(subPanelRepaintListener);
		this.inners.add(sub);
		return sub;
	}

}