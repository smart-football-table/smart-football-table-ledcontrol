package com.github.smartfootballtable.ledcontrol;

import static com.github.smartfootballtable.ledcontrol.Color.BLACK;
import static com.github.smartfootballtable.ledcontrol.LedControl.FPS.framesPerSecond;
import static java.lang.Boolean.TRUE;
import static java.util.Arrays.stream;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.github.smartfootballtable.ledcontrol.LedControl.MessageWithTopic;
import com.github.smartfootballtable.ledcontrol.panel.Panel;

public class LedControl implements Consumer<MessageWithTopic> {

	public static class FPS {

		private final int fps;

		public FPS(int fps) {
			this.fps = fps;
		}

		public static FPS framesPerSecond(int fps) {
			return new FPS(fps);
		}

		public long sleepTime(TimeUnit timeUnit) {
			return timeUnit.convert(NANOSECONDS.convert(1, SECONDS) / fps, NANOSECONDS);
		}

	}

	public static class DefaultAnimator implements Animator {

		public class DefaultAnimatorTask implements AnimatorTask {

			private final Runnable runnable;

			public DefaultAnimatorTask(Runnable runnable) {
				this.runnable = runnable;
				runnables.add(runnable);
			}

			@Override
			public void stop() {
				runnables.remove(this.runnable);
			}

		}

		private final List<Runnable> runnables = new CopyOnWriteArrayList<>();
		private final long sleepMillis;
		private volatile boolean interrupted;

		public DefaultAnimator(FPS fps) {
			sleepMillis = fps.sleepTime(MILLISECONDS);
			runAsync(() -> {
				while (!interrupted) {
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
						interrupted = true;
					}
				}

			});
		}

		@Override
		public void shutdown() {
			interrupted = true;
		}

		@Override
		public AnimatorTask start(Runnable runnable) {
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

		public static class TopicIsEqualTo implements Predicate<MessageWithTopic> {

			private final String topic;

			private TopicIsEqualTo(String topic) {
				this.topic = topic;
			}

			@Override
			public boolean test(MessageWithTopic message) {
				return Objects.equals(topic, message.getTopic());
			}

			public static TopicIsEqualTo topicIsEqualTo(String topic) {
				return new TopicIsEqualTo(topic);
			}

		}

		public static class TopicStartsWith implements Predicate<MessageWithTopic> {

			private final String topic;

			private TopicStartsWith(String topic) {
				this.topic = topic;
			}

			@Override
			public boolean test(MessageWithTopic message) {
				return message.getTopic().startsWith(topic);
			}

			public String suffix(String topic) {
				return topic.substring(this.topic.length());
			}

			public static MessageWithTopic.TopicStartsWith topicStartWith(String prefix) {
				return new TopicStartsWith(prefix);
			}

		}

	}

	public static interface ChainElement {
		boolean handle(MessageWithTopic message, Animator animator);

		Predicate<MessageWithTopic> condition();
	}

	private final Proto proto;
	private final Color[] buffer;

	private final List<ChainElement> elements = new ArrayList<>();
	private final Animator animator;

	public LedControl(Panel panel, OutputStream outputStream) {
		this(panel, outputStream, new DefaultAnimator(framesPerSecond(25)));
	}

	public LedControl(Panel panel, OutputStream outputStream, Animator animator) {
		this.proto = Proto.forFrameSizes(outputStream, panel.getWidth() * panel.getHeight());
		this.buffer = new Color[panel.getWidth() * panel.getHeight()];
		this.animator = animator;
		panel.addRepaintListener(this::repaint);
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
		elements.stream().map(e -> tryHandle(e, message)).anyMatch(TRUE::equals);
	}

	private boolean tryHandle(ChainElement element, MessageWithTopic message) {
		try {
			return element.handle(message, animator);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public static class NamedChainedElement implements ChainElement {

		private final Predicate<MessageWithTopic> condition;
		private final Consumer<MessageWithTopic> consumer;

		private NamedChainedElement(Predicate<MessageWithTopic> condition, Consumer<MessageWithTopic> consumer) {
			this.condition = condition;
			this.consumer = consumer;
		}

		@Override
		public boolean handle(MessageWithTopic message, Animator animator) {
			boolean accept = condition.test(message);
			if (accept) {
				consumer.accept(message);
			}
			return accept;
		}

		@Override
		public Predicate<MessageWithTopic> condition() {
			return this.condition;
		}
	}

	public class When {

		private final Predicate<MessageWithTopic> condition;

		public When(Predicate<MessageWithTopic> predicate) {
			this.condition = predicate;
		}

		public LedControl then(Consumer<MessageWithTopic> consumer) {
			add(new NamedChainedElement(condition, consumer));
			return LedControl.this;
		}

	}

	public When when(Predicate<MessageWithTopic> predicate) {
		return new When(predicate);
	}

	public LedControl addAll(ChainElement... elements) {
		stream(elements).forEach(this::add);
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
