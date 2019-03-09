package ledcontrol.panel;

import static java.awt.Color.BLACK;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class StackedPanel extends Panel {

	public interface OverlayStrategy {

		OverlayStrategy DEFAULT = opaque(null);

		static OverlayStrategy opaque(Color transparentColor) {
			return new OverlayStrategy() {

				@Override
				public void copy(int x, int y, Color newColor, Panel target) {
					if (!Objects.equals(transparentColor, newColor)) {
						target.setColor(x, y, newColor);
					}
				}

			};
		}

		void copy(int x, int y, Color color, Panel target);

	}

	private final List<Panel> inners = new ArrayList<>();
	private final RepaintListener subPanelRepaintListener;

	private final List<RepaintListener> repaintListeners = new CopyOnWriteArrayList<>();

	public StackedPanel(int width, int height) {
		super(width, height);
		this.subPanelRepaintListener = panel -> mixTo(this);
	}

	@Override
	public void addRepaintListener(RepaintListener listener) {
		this.repaintListeners.add(listener);
	}

	private void mixTo(Panel target) {
		clear();
		for (Panel inner : inners) {
			inner.copyTo(target);
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
		return createSubPanel(OverlayStrategy.DEFAULT);
	}

	public Panel createSubPanel(OverlayStrategy overlayStrategy) {
		Panel sub = new Panel(getWidth(), getHeight());
		sub.setOverlayStrategy(overlayStrategy);
		sub.addRepaintListener(subPanelRepaintListener);
		this.inners.add(sub);
		return sub;
	}

}