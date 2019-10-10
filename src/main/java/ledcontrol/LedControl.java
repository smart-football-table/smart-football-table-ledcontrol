package ledcontrol;

import static java.awt.Color.BLACK;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.function.Predicate.isEqual;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import ledcontrol.LedControl.MessageWithTopic;
import ledcontrol.panel.Panel;

public class LedControl implements Consumer<MessageWithTopic> {

	public static class DefaultAnimator implements Animator {

		public class DefaultAnimatorTask implements AnimatorTask {

			private final Runnable runnable;

			public DefaultAnimatorTask(Runnable runnable) {
				this.runnable = runnable;
			}

			@Override
			public void stop() {
				runnables.remove(this.runnable);
			}

		}

		private final List<Runnable> runnables = new CopyOnWriteArrayList<>();
		private final long sleepMillis;

		public DefaultAnimator(int fps) {
			sleepMillis = SECONDS.toMillis(1) / fps;
			runAsync(() -> {
				while (true) {
					long startTime = System.currentTimeMillis();
					for (Runnable runnable : runnables) {
						try {
							runnable.run();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					try {
						MILLISECONDS.sleep(sleepMillis - (System.currentTimeMillis() - startTime));
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}

			});
		}

		@Override
		public AnimatorTask start(Runnable runnable) {
			runnables.add(runnable);
			return new DefaultAnimatorTask(runnable);
		}
	}

	public static class MessageWithTopic {

		private final String topic;
		private final String payload;

		public MessageWithTopic(String topic, String payload) {
			this.topic = topic;
			this.payload = payload;
		}

		public String getTopic() {
			return topic;
		}

		public String getPayload() {
			return payload;
		}

		public static Predicate<MessageWithTopic> topicIsEqualTo(String topic) {
			return matches(topic, MessageWithTopic::getTopic);
		}

		public static Predicate<MessageWithTopic> topicStartWith(String prefix) {
			return m -> ((Function<MessageWithTopic, String>) MessageWithTopic::getTopic).apply(m).startsWith(prefix);
		}

		public static <T> Predicate<MessageWithTopic> matches(T value, Function<MessageWithTopic, T> f) {
			return m -> isEqual(value).test(f.apply(m));
		}

	}

	public static class ChainElement implements Consumer<MessageWithTopic> {

		private final Predicate<MessageWithTopic> condition;
		private final Consumer<MessageWithTopic> consumer;

		public ChainElement(Predicate<MessageWithTopic> condition, Consumer<MessageWithTopic> consumer) {
			this.condition = condition;
			this.consumer = consumer;
		}

		public boolean canHandle(MessageWithTopic message) {
			return condition.test(message);
		}

		@Override
		public void accept(MessageWithTopic message) {
			consumer.accept(message);
		}

	}

	private final Proto proto;
	private final Color[] buffer;

	private final List<ChainElement> elements = new ArrayList<>();
	private final Animator animator;

	public LedControl(Panel panel, OutputStream outputStream) {
		this(panel, outputStream, new DefaultAnimator(25));
	}

	public LedControl(Panel panel, OutputStream outputStream, Animator animator) {
		this.proto = Proto.forFrameSizes(outputStream, panel.getWidth() * panel.getHeight());
		this.buffer = new Color[panel.getWidth() * panel.getHeight()];
		this.animator = animator;
		panel.addRepaintListener(p -> repaint(p));
		panel.repaint();
	}

	private void repaint(Panel panel) {
		int width = panel.getWidth();
		int height = panel.getHeight();
		Color[][] colors = panel.getColors();
		synchronized (buffer) {
			try {
				for (int y = 0; y < height; y++) {
					Color[] row = colors[y];
					for (int x = 0; x < width; x++) {
						Color color = row[x];
						buffer[width * y + x] = color == null ? BLACK : color;
					}
				}
				proto.write(buffer);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public void accept(MessageWithTopic message) {
		for (ChainElement element : elements) {
			if (element.canHandle(message)) {
				try {
					handleMessage(element, message);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	protected void handleMessage(ChainElement element, MessageWithTopic message) {
		element.accept(message);
	}

	public LedControl addAll(ChainElement... elements) {
		Collections.addAll(this.elements, elements);
		return this;
	}

	public LedControl add(ChainElement chainElement) {
		elements.add(chainElement);
		return this;
	}

	public Animator getAnimator() {
		return animator;
	}

}
