package ledcontrol;

import static io.moquette.BrokerConstants.HOST_PROPERTY_NAME;
import static io.moquette.BrokerConstants.PORT_PROPERTY_NAME;
import static java.awt.Color.BLACK;
import static java.awt.Color.BLUE;
import static java.awt.Color.RED;
import static java.awt.Color.WHITE;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import io.moquette.server.Server;
import io.moquette.server.config.MemoryConfig;
import ledcontrol.Animator.AnimatorTask;
import ledcontrol.panel.StackedPanel;
import ledcontrol.runner.SystemRunner.Configurator;
import ledcontrol.scene.IdleScene;

public class SystemIntegrationIT {

	private static final String LOCALHOST = "localhost";
	private static final Color ___ = BLACK;
	private static final Color COLOR_TEAM_LEFT = BLUE;
	private static final Color COLOR_TEAM_RIGHT = RED;

	private IdleScene idleScene = mock(IdleScene.class);

	private int brokerPort;

	private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

	private final StackedPanel panel = new StackedPanel(5, 2);

	private Server server;
	private TheSystem theSystem;
	private IMqttClient secondClient;

	@Before
	public void setup() throws IOException, MqttException {
		brokerPort = randomPort();
		server = newMqttServer(LOCALHOST, brokerPort);
		secondClient = newMqttClient(LOCALHOST, brokerPort, "client2");
	}

	private int randomPort() throws IOException {
		try (ServerSocket socket = new ServerSocket(0);) {
			return socket.getLocalPort();
		}
	}

	private Server newMqttServer(String host, int port) throws IOException {
		Server server = new Server();
		Properties properties = new Properties();
		properties.setProperty(HOST_PROPERTY_NAME, host);
		properties.setProperty(PORT_PROPERTY_NAME, String.valueOf(port));
		server.startServer(new MemoryConfig(properties));
		return server;
	}

	private MqttClient newMqttClient(String host, int port, String id) throws MqttException, MqttSecurityException {
		MqttClient client = new MqttClient("tcp://" + host + ":" + port, id, new MemoryPersistence());
		client.connect();
		return client;
	}

	@After
	public void tearDown() throws MqttException {
		secondClient.disconnect();
		secondClient.close();
		theSystem.close();
		server.stopServer();
	}

	@Test
	public void teamLeftScores() throws MqttSecurityException, MqttException, InterruptedException, IOException {
		givenTheSystemConnectedToBroker(LOCALHOST, brokerPort);
		whenMessageIsReceived(LOCALHOST, brokerPort, "score", score(1, 0));
		assertThat(lastPanelState(), is(new Color[][] { //
				{ COLOR_TEAM_LEFT, ___, ___, ___, ___ }, //
				{ COLOR_TEAM_LEFT, ___, ___, ___, ___ }, //
		}));
	}

	@Test
	public void teamLeftScoresTwice() throws MqttSecurityException, MqttException, InterruptedException, IOException {
		givenTheSystemConnectedToBroker(LOCALHOST, brokerPort);
		whenMessageIsReceived(LOCALHOST, brokerPort, "score", score(1, 0));
		whenMessageIsReceived(LOCALHOST, brokerPort, "score", score(2, 0));
		assertThat(lastPanelState(), is(new Color[][] { //
				{ COLOR_TEAM_LEFT, COLOR_TEAM_LEFT, ___, ___, ___ }, //
				{ COLOR_TEAM_LEFT, COLOR_TEAM_LEFT, ___, ___, ___ }, //
		}));
	}

	private String score(int... scores) {
		return "{ \"score\": [ " + IntStream.of(scores).mapToObj(String::valueOf).collect(Collectors.joining(", "))
				+ " ] }";
	}

	@Test
	public void flashesOnFoul() throws MqttSecurityException, MqttException, InterruptedException, IOException {
		givenTheSystemConnectedToBroker(LOCALHOST, brokerPort);
		whenMessageIsReceived(LOCALHOST, brokerPort, "foul", "");
		assertThat(lastPanelState(), is(new Color[][] { //
				{ WHITE, WHITE, WHITE, WHITE, WHITE }, //
				{ WHITE, WHITE, WHITE, WHITE, WHITE }, //
		}));
	}

	@Test
	public void animationOnIdle() throws MqttSecurityException, MqttException, InterruptedException, IOException {
		givenTheSystemConnectedToBroker(LOCALHOST, brokerPort);
		whenMessageIsReceived(LOCALHOST, brokerPort, "idle", "{ \"idle\": true }");
		MILLISECONDS.sleep(40);
		verify(idleScene).startAnimation(Mockito.any(Animator.class));
	}

	@Test
	public void canStartAndStopTasks() throws InterruptedException, MqttSecurityException, MqttException {
		givenTheSystemConnectedToBroker(LOCALHOST, brokerPort);
		AtomicInteger incremntor = new AtomicInteger(0);
		Runnable incremetor = () -> incremntor.incrementAndGet();
		assertThat(incremntor.get(), is(0));
		AnimatorTask task = theSystem.getAnimator().start(incremetor);
		MILLISECONDS.sleep(40 * 2);
		assertThat(incremntor.get(), is(not(0)));

		task.stop();
		int currentValue = incremntor.get();
		MILLISECONDS.sleep(40 * 2);
		assertThat(incremntor.get(), is(currentValue));
	}

	private Color[][] lastPanelState() throws IOException {
		return toColors(last(receivedFrames()));
	}

	private Color[][] toColors(Tpm2Frame frame) {
		int height = panel.getHeight();
		int width = panel.getWidth();
		Color[][] colors = new Color[height][width];
		for (int y = 0; y < height; y++) {
			System.arraycopy(frame.getColors(), y * width, colors[y], 0, width);
		}
		return colors;
	}

	private List<Tpm2Frame> receivedFrames() throws IOException {
		InputStream is = new ByteArrayInputStream(outputStream.toByteArray());
		List<Tpm2Frame> frames = new ArrayList<Tpm2Frame>();
		while (is.available() > 0) {
			frames.add(Tpm2Frame.fromStream(is));
		}
		return frames;
	}

	private static <T> T last(List<T> list) throws IOException {
		return list.get(list.size() - 1);
	}

	private void whenMessageIsReceived(String host, int port, String topic, String message)
			throws MqttSecurityException, MqttException, InterruptedException {
		secondClient.publish(topic, new MqttMessage(message.getBytes()));
		// TODO use lock/condition
		TimeUnit.MILLISECONDS.sleep(250);
	}

	private void givenTheSystemConnectedToBroker(String host, int port) throws MqttSecurityException, MqttException {
		theSystem = new Configurator() {
			protected ledcontrol.scene.IdleScene idleScene(ledcontrol.panel.Panel idlePanel) {
				return idleScene;
			};
		}.configure(new TheSystem(host, port, panel, outputStream), panel);
	}

}
