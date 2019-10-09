package ledcontrol.mqtt;

import java.io.Closeable;
import java.util.function.Consumer;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import ledcontrol.LedControl.MessageWithTopic;

public class MqttAdapter implements Closeable {

	private final IMqttClient mqttClient;
	private final Consumer<MessageWithTopic> consumer;

	public MqttAdapter(String host, int port, Consumer<MessageWithTopic> consumer)
			throws MqttSecurityException, MqttException {
		this.mqttClient = makeMqttClient(host, port);
		this.consumer = consumer;
		subscribe();
	}

	private IMqttClient makeMqttClient(String host, int port) throws MqttException, MqttSecurityException {
		IMqttClient client = new MqttClient("tcp://" + host + ":" + port, "theSystemClient", new MemoryPersistence());
		client.connect(mqttConnectOptions());
		client.setCallback(callback(client));
		return client;
	}

	private MqttConnectOptions mqttConnectOptions() {
		MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
		mqttConnectOptions.setAutomaticReconnect(true);
		return mqttConnectOptions;
	}

	private MqttCallbackExtended callback(IMqttClient client) {
		return new MqttCallbackExtended() {
			@Override
			public void connectionLost(Throwable cause) {
				System.out.println("Connection to " + client.getServerURI() + " lost");
			}

			@Override
			public void messageArrived(String topic, org.eclipse.paho.client.mqttv3.MqttMessage message) {
			}

			@Override
			public void deliveryComplete(IMqttDeliveryToken token) {
			}

			@Override
			public void connectComplete(boolean reconnect, String serverURI) {
				System.out.println("Connection to " + serverURI + " established");
				if (reconnect) {
					try {
						subscribe();
					} catch (MqttException e) {
						e.printStackTrace();
					}
				}
			}

		};
	}

	private void subscribe() throws MqttException, MqttSecurityException {
		mqttClient.subscribe("#", (t, m) -> consumer.accept(new MessageWithTopic(t, new String(m.getPayload()))));
	}

	@Override
	public void close() {
		try {
			if (isConnected()) {
				this.mqttClient.disconnect();
			}
			this.mqttClient.close();
		} catch (MqttException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean isConnected() {
		return this.mqttClient.isConnected();
	}

}