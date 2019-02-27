package ledcontrol;

import java.awt.Color;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import ledcontrol.panel.Panel;

public class TheSystem implements Closeable {

	private final IMqttClient mqttClient;
	private final Proto proto;
	private final Color[] buffer;

	public TheSystem(String host, int port, Panel panel, OutputStream outputStream)
			throws MqttSecurityException, MqttException {
		proto = Proto.forFrameSizes(outputStream, panel.getWidth() * panel.getHeight());
		buffer = new Color[panel.getWidth() * panel.getHeight()];
		mqttClient = makeMqttClient(host, port);
		mqttClient.subscribe("#", (topic, message) -> {
			received(topic, new String(message.getPayload()));
		});
		panel.addRepaintListener(p -> repaint(p));
	}

	private void repaint(Panel panel) {
		synchronized (buffer) {
			int width = panel.getWidth();
			try {
				Color[][] colors = panel.getColors();
				for (int y = 0; y < colors.length; y++) {
					System.arraycopy(colors[y], 0, buffer, width * y, width);
				}
				proto.write(buffer);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	protected void received(String topic, String payload) {
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

}
