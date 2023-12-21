package com.github.smartfootballtable.ledcontrol;

import static com.github.smartfootballtable.ledcontrol.Color.BLACK;
import static com.github.smartfootballtable.ledcontrol.Color.decode;
import static com.github.smartfootballtable.ledcontrol.TpmDecode.decodeFrames;
import static com.github.smartfootballtable.ledcontrol.TpmDecode.toColors;
import static com.github.smartfootballtable.ledcontrol.runner.Colors.BLUE;
import static com.github.smartfootballtable.ledcontrol.runner.Colors.ORANGE;
import static com.github.smartfootballtable.ledcontrol.runner.SystemRunner.Messages.isIdle;
import static io.moquette.BrokerConstants.HOST_PROPERTY_NAME;
import static io.moquette.BrokerConstants.PORT_PROPERTY_NAME;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.setDefaultTimeout;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.apache.commons.collections4.map.HashedMap;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.github.smartfootballtable.ledcontrol.Animator.AnimatorTask;
import com.github.smartfootballtable.ledcontrol.LedControl.ChainElement;
import com.github.smartfootballtable.ledcontrol.LedControl.MessageWithTopic;
import com.github.smartfootballtable.ledcontrol.mqtt.MqttAdapter;
import com.github.smartfootballtable.ledcontrol.panel.StackedPanel;
import com.github.smartfootballtable.ledcontrol.runner.SystemRunner.Configurator;

import io.moquette.broker.Server;
import io.moquette.broker.config.MemoryConfig;

@Timeout(value = 30, unit = SECONDS)
class SystemIntegrationIT {

	private static final String LOCALHOST = "localhost";
	private static final Color COLOR_TEAM_LEFT = BLUE;
	private static final Color COLOR_TEAM_RIGHT = ORANGE;

	private int brokerPort;

	private final Map<Predicate<MessageWithTopic>, ChainElement> spies = new HashedMap<>();
	private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

	private final StackedPanel panel = new StackedPanel(5, 2);

	private Server broker;
	private MqttAdapter mqttAdapter;
	private LedControl ledControl;
	private IMqttClient secondClient;

	@BeforeEach
	void setup() throws IOException, MqttException {
		brokerPort = randomPort();
		broker = newMqttBroker(LOCALHOST, brokerPort);
		secondClient = newMqttClient(LOCALHOST, brokerPort, "secondClient" + randomUUID());
		findAnnotation(getClass(), Timeout.class).ifPresent(t -> setDefaultTimeout(t.value(), t.unit()));
	}

	private int randomPort() throws IOException {
		try (ServerSocket socket = new ServerSocket(0);) {
			return socket.getLocalPort();
		}
	}

	private Server newMqttBroker(String host, int port) throws IOException {
		Server server = new Server();
		Properties properties = new Properties();
		properties.setProperty(HOST_PROPERTY_NAME, host);
		properties.setProperty(PORT_PROPERTY_NAME, String.valueOf(port));
		server.startServer(new MemoryConfig(properties));
		return server;
	}

	private MqttClient newMqttClient(String host, int port, String id) throws MqttException, MqttSecurityException {
		MqttClient client = new MqttClient("tcp://" + host + ":" + port, id, new MemoryPersistence());
		client.connect(mqttConnectOptions());
		return client;
	}

	private MqttConnectOptions mqttConnectOptions() {
		MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
		mqttConnectOptions.setAutomaticReconnect(true);
		return mqttConnectOptions;
	}

	@AfterEach
	void tearDown() throws MqttException {
		if (secondClient.isConnected()) {
			secondClient.disconnect();
		}
		secondClient.close();
		mqttAdapter.close();
		broker.stopServer();
		ledControl.getAnimator().shutdown();
	}

	@Test
	void panelIsBlackOnSystemStart() throws MqttSecurityException, MqttException, IOException {
		givenTheSystemConnectedToBroker(LOCALHOST, brokerPort);
		assertThat(lastPanelState(), is(new Color[][] { //
				{ BLACK, BLACK, BLACK, BLACK, BLACK }, //
				{ BLACK, BLACK, BLACK, BLACK, BLACK }, //
		}));
	}

	@Test
	void flashesOnWinnerRight() throws MqttSecurityException, MqttException, InterruptedException, IOException {
		givenTheSystemConnectedToBroker(LOCALHOST, brokerPort);
		whenMessageIsReceived(LOCALHOST, brokerPort, "game/gameover", "1");
		await().until(this::lastPanelState, is(new Color[][] { //
				{ COLOR_TEAM_RIGHT, COLOR_TEAM_RIGHT, COLOR_TEAM_RIGHT, COLOR_TEAM_RIGHT, COLOR_TEAM_RIGHT }, //
				{ COLOR_TEAM_RIGHT, COLOR_TEAM_RIGHT, COLOR_TEAM_RIGHT, COLOR_TEAM_RIGHT, COLOR_TEAM_RIGHT }, //
		}));
	}

	@Test
	void animationOnIdle() throws MqttSecurityException, MqttException, InterruptedException, IOException {
		givenTheSystemConnectedToBroker(LOCALHOST, brokerPort);
		whenMessageIsReceived(LOCALHOST, brokerPort, "game/idle", "true");
		assertWasHandled(isIdle);
	}

	@Test
	void backgroundColorChanges() throws MqttSecurityException, MqttException, InterruptedException, IOException {
		givenTheSystemConnectedToBroker(LOCALHOST, brokerPort);
		whenMessageIsReceived(LOCALHOST, brokerPort, "leds/backgroundlight/color", "#1188CC");
		Color _1188CC = decode("#1188CC");
		await().until(this::lastPanelState, is(new Color[][] { //
				{ _1188CC, _1188CC, _1188CC, _1188CC, _1188CC }, //
				{ _1188CC, _1188CC, _1188CC, _1188CC, _1188CC }, //
		}));
	}

	@Test
	void foregroundColorChanges() throws MqttSecurityException, MqttException, InterruptedException, IOException {
		givenTheSystemConnectedToBroker(LOCALHOST, brokerPort);
		whenMessageIsReceived(LOCALHOST, brokerPort, "leds/backgroundlight/color", "#1188CC");
		whenMessageIsReceived(LOCALHOST, brokerPort, "leds/foregroundlight/color", "#22AADD");
		Color _22AADD = decode("#22AADD");
		await().until(this::lastPanelState, is(new Color[][] { //
				{ _22AADD, _22AADD, _22AADD, _22AADD, _22AADD }, //
				{ _22AADD, _22AADD, _22AADD, _22AADD, _22AADD }, //
		}));
	}

	@Test
	void foregroundColorOverridesBackgroundColor()
			throws MqttSecurityException, MqttException, InterruptedException, IOException {
		givenTheSystemConnectedToBroker(LOCALHOST, brokerPort);
		whenMessageIsReceived(LOCALHOST, brokerPort, "leds/backgroundlight/color", "#1188CC");
		whenMessageIsReceived(LOCALHOST, brokerPort, "leds/foregroundlight/color", "#22AADD");
		Color _22AADD = decode("#22AADD");
		await().until(this::lastPanelState, is(new Color[][] { //
				{ _22AADD, _22AADD, _22AADD, _22AADD, _22AADD }, //
				{ _22AADD, _22AADD, _22AADD, _22AADD, _22AADD }, //
		}));
	}

	@Test
	void foregroundColor_BlackIsTransparent()
			throws MqttSecurityException, MqttException, InterruptedException, IOException {
		givenTheSystemConnectedToBroker(LOCALHOST, brokerPort);
		whenMessageIsReceived(LOCALHOST, brokerPort, "leds/backgroundlight/color", "#1188CC");
		whenMessageIsReceived(LOCALHOST, brokerPort, "leds/foregroundlight/color", "#22AADD");
		whenMessageIsReceived(LOCALHOST, brokerPort, "leds/foregroundlight/color", "#000000");
		Color _1188cc = decode("#1188CC");
		await().until(this::lastPanelState, is(new Color[][] { //
				{ _1188cc, _1188cc, _1188cc, _1188cc, _1188cc }, //
				{ _1188cc, _1188cc, _1188cc, _1188cc, _1188cc }, //
		}));
	}

	@Test
	void canStartAndStopTasks() throws InterruptedException, MqttSecurityException, MqttException {
		givenTheSystemConnectedToBroker(LOCALHOST, brokerPort);
		AtomicInteger atomicInteger = new AtomicInteger(0);
		Runnable incremetor = () -> atomicInteger.incrementAndGet();
		assertThat(atomicInteger.get(), is(0));
		AnimatorTask task = ledControl.getAnimator().start(incremetor);
		await().until(() -> atomicInteger.get(), is(not(0)));

		task.stop();
		int currentValue = atomicInteger.get();
		await().until(() -> atomicInteger.get(), is(currentValue));
	}

	@Test
	void doesReconnectToBrokerAndResubscribeToTopics()
			throws InterruptedException, MqttSecurityException, MqttException, IOException, TimeoutException {
		givenTheSystemConnectedToBroker(LOCALHOST, brokerPort);
		broker.stopServer();

		broker = newMqttBroker(LOCALHOST, brokerPort);
		await().atMost(10, SECONDS).until(mqttAdapter::isConnected);

		// does the reconnected client subscribe to the topics again?
		await().atMost(10, SECONDS).until(secondClient::isConnected);
		whenMessageIsReceived(LOCALHOST, brokerPort, "game/idle", "true");
		assertWasHandled(isIdle);
	}

	private Color[][] lastPanelState() throws IOException {
		return toColors(last(receivedFrames()), panel.getHeight(), panel.getWidth());
	}

	private List<Tpm2Frame> receivedFrames() throws IOException {
		return decodeFrames(outputStream.toByteArray());
	}

	private void assertWasHandled(Predicate<MessageWithTopic> condition) {
		verify(spies.get(condition), timeout(500)).handle(any(MessageWithTopic.class), any(Animator.class));
	}

	private static <T> T last(List<T> list) {
		return list.get(list.size() - 1);
	}

	private void whenMessageIsReceived(String host, int port, String topic, String message)
			throws MqttSecurityException, MqttException, InterruptedException {
		secondClient.publish(topic, new MqttMessage(message.getBytes()));
	}

	private void givenTheSystemConnectedToBroker(String host, int port) throws MqttSecurityException, MqttException {
		ledControl = new Configurator(COLOR_TEAM_LEFT, COLOR_TEAM_RIGHT) //
				.configure(new LedControl(panel, outputStream) {
					@Override
					public LedControl add(ChainElement element) {
						ChainElement spy = spy(element);
						spies.put(element.condition(), spy);
						return super.add(spy);
					}
				}, panel);
		mqttAdapter = new MqttAdapter(host, port, ledControl);
	}

}
