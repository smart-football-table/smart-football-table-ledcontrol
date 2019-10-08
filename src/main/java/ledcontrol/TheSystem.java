package ledcontrol;

import static java.awt.Color.BLACK;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.function.Predicate.isEqual;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import ledcontrol.TheSystem.MessageWithTopic;
import ledcontrol.panel.Panel;

public class TheSystem implements Consumer<MessageWithTopic> {

	public static class TheSystemAnimator implements Animator {

		public class TheSystemAnimatorTask implements AnimatorTask {

			private final Runnable runnable;

			public TheSystemAnimatorTask(Runnable runnable) {
				this.runnable = runnable;
			}

			@Override
			public void stop() {
				callables.remove(this.runnable);
			}

		}

		private final List<Runnable> callables = new CopyOnWriteArrayList<>();
		private final long sleepMillis;

		public TheSystemAnimator(int fps) {
			sleepMillis = SECONDS.toMillis(1) / fps;
			runAsync(() -> {
				while (true) {
					long startTime = System.currentTimeMillis();
					for (Runnable runnable : callables) {
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
			callables.add(runnable);
			return new TheSystemAnimatorTask(runnable);
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

	private final Proto proto;
	private final Color[] buffer;

	private final Map<Predicate<MessageWithTopic>, Consumer<MessageWithTopic>> conditions = new HashMap<>();
	private final int FPS = 25;
	private final Animator animator = new TheSystemAnimator(FPS);

	public TheSystem(Panel panel, OutputStream outputStream) {
		this.proto = Proto.forFrameSizes(outputStream, panel.getWidth() * panel.getHeight());
		this.buffer = new Color[panel.getWidth() * panel.getHeight()];
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
		for (Entry<Predicate<MessageWithTopic>, Consumer<MessageWithTopic>> entry : conditions.entrySet()) {
			if (entry.getKey().test(message)) {
				try {
					handleMessage(entry.getValue(), message);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	protected void handleMessage(Consumer<MessageWithTopic> consumer, MessageWithTopic message) {
		consumer.accept(message);
	}

	public class When {

		private final Predicate<MessageWithTopic> predicate;

		public When(Predicate<MessageWithTopic> predicate) {
			this.predicate = predicate;
		}

		public TheSystem then(Consumer<MessageWithTopic> consumer) {
			conditions.put(predicate, consumer);
			return TheSystem.this;
		}

	}

	public When when(Predicate<MessageWithTopic> predicate) {
		return new When(predicate);
	}

	public Animator getAnimator() {
		return animator;
	}

}
