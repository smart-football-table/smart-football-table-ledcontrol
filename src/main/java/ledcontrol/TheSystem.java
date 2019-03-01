package ledcontrol;

import static java.awt.Color.BLACK;

import java.awt.Color;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import ledcontrol.panel.Panel;

public class TheSystem implements Closeable {

	public static class Animator {
		public void start(Callable<Void> callable) {
			for (int i = 0; i < 2; i++) {
				try {
					callable.call();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static class MqttMessage {

		private String topic;
		private String payload;

		public MqttMessage(String topic, String payload) {
			this.topic = topic;
			this.payload = payload;
		}

		public String getTopic() {
			return topic;
		}

		public String getPayload() {
			return payload;
		}

		public static Predicate<TheSystem.MqttMessage> isTopic(String topic) {
			return m -> m.getTopic().equals(topic);
		}

		public static Predicate<TheSystem.MqttMessage> isPayload(String payload) {
			return m -> m.getPayload().equals(payload);
		}

	}

	private final IMqttClient mqttClient;
	private final Proto proto;
	private final Color[] buffer;

	public TheSystem(String host, int port, Panel panel, OutputStream outputStream)
			throws MqttSecurityException, MqttException {
		proto = Proto.forFrameSizes(outputStream, panel.getWidth() * panel.getHeight());
		buffer = new Color[panel.getWidth() * panel.getHeight()];
		mqttClient = makeMqttClient(host, port);
		mqttClient.subscribe("#", (topic, message) -> {
			received(new MqttMessage(topic, new String(message.getPayload())));
		});
		panel.addRepaintListener(p -> repaint(p));
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

	private final Map<Predicate<MqttMessage>, Consumer<MqttMessage>> conditions = new HashMap<>();
	private final Animator animator = new Animator();

	private void received(MqttMessage message) {
		for (Entry<Predicate<MqttMessage>, Consumer<MqttMessage>> entry : conditions.entrySet()) {
			if (entry.getKey().test(message)) {
				try {
					entry.getValue().accept(message);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void whenThen(Predicate<MqttMessage> predicate, Consumer<MqttMessage> consumer) {
		conditions.put(predicate, consumer);
	}

	@Override
	public void close() {
		try {
			this.mqttClient.disconnect();
			this.mqttClient.close();
		} catch (MqttException e) {
			throw new RuntimeException(e);
		}
	}

	private IMqttClient makeMqttClient(String host, int port) throws MqttException, MqttSecurityException {
		IMqttClient client = new MqttClient("tcp://" + host + ":" + port, "theSystemClient", new MemoryPersistence());
		client.connect();
		return client;
	}

	public Animator getAnimator() {
		return animator;
	}

}
