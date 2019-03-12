package ledcontrol;

import static io.moquette.BrokerConstants.HOST_PROPERTY_NAME;
import static io.moquette.BrokerConstants.PORT_PROPERTY_NAME;
import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static java.awt.Color.decode;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static ledcontrol.SystemIntegrationIT.Waiter.waitFor;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.rules.Timeout.seconds;
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
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.mockito.Mockito;

import io.moquette.server.Server;
import io.moquette.server.config.MemoryConfig;
import ledcontrol.Animator.AnimatorTask;
import ledcontrol.panel.Panel;
import ledcontrol.panel.StackedPanel;
import ledcontrol.runner.Colors;
import ledcontrol.runner.SystemRunner.Configurator;
import ledcontrol.scene.IdleScene;
import ledcontrol.scene.ScoreScene;

public class SystemIntegrationIT {

	public static class Waiter {

		private final int timeout;
		private final TimeUnit unit;

		public Waiter(int timeout, TimeUnit unit) {
			this.timeout = timeout;
			this.unit = unit;
		}

		public static Waiter waitFor(int timeout, TimeUnit unit) {
			return new Waiter(timeout, unit);
		}

		public void until(Supplier<Boolean> supplier, boolean expected) throws InterruptedException, TimeoutException {
			long start = System.currentTimeMillis();
			while (supplier.get() != expected) {
				if (System.currentTimeMillis() > start + unit.toMillis(timeout)) {
					throw new TimeoutException();
				}
				sleep();
			}
		}

		private void sleep() throws InterruptedException {
			MILLISECONDS.sleep(100);
		}

	}

	@Rule
	public Timeout timeout = seconds(30);

	private static final String LOCALHOST = "localhost";
	private static final Color COLOR_TEAM_LEFT = Colors.BLUE;
	private static final Color COLOR_TEAM_RIGHT = Colors.ORANGE;

	private IdleScene idleScene = mock(IdleScene.class);

	private int brokerPort;

	private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

	private final StackedPanel panel = new StackedPanel(5, 2);

	private Server server;
	private TheSystem theSystem;
	private IMqttClient secondClient;

	private final Lock lock = new ReentrantLock();
	private final Condition consumed = lock.newCondition();
	private int unconsumedMessages;

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
		client.connect(mqttConnectOptions());
		return client;
	}

	private MqttConnectOptions mqttConnectOptions() {
		MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
		mqttConnectOptions.setAutomaticReconnect(true);
		return mqttConnectOptions;
	}

	@After
	public void tearDown() throws MqttException {
		if (secondClient.isConnected()) {
			secondClient.disconnect();
		}
		secondClient.close();
		theSystem.close();
		server.stopServer();
	}

	@Test
	public void panelIsBlackOnSystemStart() throws MqttSecurityException, MqttException, IOException {
		givenTheSystemConnectedToBroker(LOCALHOST, brokerPort);
		assertThat(lastPanelState(), is(new Color[][] { //
				{ BLACK, BLACK, BLACK, BLACK, BLACK }, //
				{ BLACK, BLACK, BLACK, BLACK, BLACK }, //
		}));
	}

	@Test
	public void teamLeftScores() throws MqttSecurityException, MqttException, InterruptedException, IOException {
		givenTheSystemConnectedToBroker(LOCALHOST, brokerPort);
		whenMessageIsReceived(LOCALHOST, brokerPort, "game/score", score(1, 0));
		assertThat(lastPanelState(), is(new Color[][] { //
				{ COLOR_TEAM_LEFT, BLACK, BLACK, BLACK, BLACK }, //
				{ COLOR_TEAM_LEFT, BLACK, BLACK, BLACK, BLACK }, //
		}));
	}

	@Test
	public void teamLeftScoresTwice() throws MqttSecurityException, MqttException, InterruptedException, IOException {
		givenTheSystemConnectedToBroker(LOCALHOST, brokerPort);
		whenMessageIsReceived(LOCALHOST, brokerPort, "game/score", score(1, 0));
		whenMessageIsReceived(LOCALHOST, brokerPort, "game/score", score(2, 0));
		assertThat(lastPanelState(), is(new Color[][] { //
				{ COLOR_TEAM_LEFT, COLOR_TEAM_LEFT, BLACK, BLACK, BLACK }, //
				{ COLOR_TEAM_LEFT, COLOR_TEAM_LEFT, BLACK, BLACK, BLACK }, //
		}));
	}

	private String score(int... scores) {
		return "{ \"score\": [ " + IntStream.of(scores).mapToObj(String::valueOf).collect(Collectors.joining(", "))
				+ " ] }";
	}

	@Test
	public void flashesOnFoul() throws MqttSecurityException, MqttException, InterruptedException, IOException {
		givenTheSystemConnectedToBroker(LOCALHOST, brokerPort);
		whenMessageIsReceived(LOCALHOST, brokerPort, "game/foul", "");
		MILLISECONDS.sleep(40);
		assertThat(lastPanelState(), is(new Color[][] { //
				{ WHITE, WHITE, WHITE, WHITE, WHITE }, //
				{ WHITE, WHITE, WHITE, WHITE, WHITE }, //
		}));
	}

	@Test
	public void animationOnIdle() throws MqttSecurityException, MqttException, InterruptedException, IOException {
		givenTheSystemConnectedToBroker(LOCALHOST, brokerPort);
		whenMessageIsReceived(LOCALHOST, brokerPort, "game/idle", "{ \"idle\": true }");
		MILLISECONDS.sleep(40);
		verify(idleScene).startAnimation(Mockito.any(Animator.class));
	}

	@Test
	public void backgroundColorChanges()
			throws MqttSecurityException, MqttException, InterruptedException, IOException {
		givenTheSystemConnectedToBroker(LOCALHOST, brokerPort);
		whenMessageIsReceived(LOCALHOST, brokerPort, "leds/backgroundlight/color", "#1188CC");
		MILLISECONDS.sleep(40);
		Color _1188CC = decode("#1188CC");
		assertThat(lastPanelState(), is(new Color[][] { //
				{ _1188CC, _1188CC, _1188CC, _1188CC, _1188CC }, //
				{ _1188CC, _1188CC, _1188CC, _1188CC, _1188CC }, //
		}));
	}

	@Test
	public void foregroundColorChanges()
			throws MqttSecurityException, MqttException, InterruptedException, IOException {
		givenTheSystemConnectedToBroker(LOCALHOST, brokerPort);
		whenMessageIsReceived(LOCALHOST, brokerPort, "leds/backgroundlight/color", "#1188CC");
		whenMessageIsReceived(LOCALHOST, brokerPort, "leds/foregroundlight/color", "#22AADD");
		MILLISECONDS.sleep(40);
		Color _22AADD = decode("#22AADD");
		assertThat(lastPanelState(), is(new Color[][] { //
				{ _22AADD, _22AADD, _22AADD, _22AADD, _22AADD }, //
				{ _22AADD, _22AADD, _22AADD, _22AADD, _22AADD }, //
		}));
	}

	@Test
	public void foregroundColorOverridesBackgroundColor()
			throws MqttSecurityException, MqttException, InterruptedException, IOException {
		givenTheSystemConnectedToBroker(LOCALHOST, brokerPort);
		whenMessageIsReceived(LOCALHOST, brokerPort, "leds/backgroundlight/color", "#1188CC");
		whenMessageIsReceived(LOCALHOST, brokerPort, "leds/foregroundlight/color", "#22AADD");
		MILLISECONDS.sleep(40);
		Color _22AADD = decode("#22AADD");
		assertThat(lastPanelState(), is(new Color[][] { //
				{ _22AADD, _22AADD, _22AADD, _22AADD, _22AADD }, //
				{ _22AADD, _22AADD, _22AADD, _22AADD, _22AADD }, //
		}));
	}

	@Test
	public void foregroundColor_BlackIsTransparent()
			throws MqttSecurityException, MqttException, InterruptedException, IOException {
		givenTheSystemConnectedToBroker(LOCALHOST, brokerPort);
		whenMessageIsReceived(LOCALHOST, brokerPort, "leds/backgroundlight/color", "#1188CC");
		whenMessageIsReceived(LOCALHOST, brokerPort, "leds/foregroundlight/color", "#22AADD");
		whenMessageIsReceived(LOCALHOST, brokerPort, "leds/foregroundlight/color", "#000000");
		MILLISECONDS.sleep(40);
		Color _1188cc = decode("#1188CC");
		assertThat(lastPanelState(), is(new Color[][] { //
				{ _1188cc, _1188cc, _1188cc, _1188cc, _1188cc }, //
				{ _1188cc, _1188cc, _1188cc, _1188cc, _1188cc }, //
		}));
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

	@Test
	public void doesReconnectToBrokerAndResubscribeToTopics()
			throws InterruptedException, MqttSecurityException, MqttException, IOException, TimeoutException {
		givenTheSystemConnectedToBroker(LOCALHOST, brokerPort);
		server.stopServer();
		waitFor(10, SECONDS).until(() -> theSystem.isConnected(), false);

		server = newMqttServer(LOCALHOST, brokerPort);
		waitFor(10, SECONDS).until(() -> theSystem.isConnected(), true);

		// does the reconnected client subscribe to the topics again?
		waitFor(10, SECONDS).until(() -> secondClient.isConnected(), true);
		whenMessageIsReceived(LOCALHOST, brokerPort, "game/idle", "{ \"idle\": true }");
		MILLISECONDS.sleep(40);
		verify(idleScene).startAnimation(Mockito.any(Animator.class));
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
		lock.lock();
		try {
			unconsumedMessages++;
			secondClient.publish(topic, new MqttMessage(message.getBytes()));
			while (unconsumedMessages > 0) {
				consumed.await();
			}
		} finally {
			lock.unlock();
		}
	}

	private void givenTheSystemConnectedToBroker(String host, int port) throws MqttSecurityException, MqttException {
		theSystem = new Configurator() {

			protected ScoreScene goalScene(Panel goalPanel) {
				return super.goalScene(goalPanel).pixelsPerGoal(1).spaceDots(0);
			}

			protected ledcontrol.scene.IdleScene idleScene(ledcontrol.panel.Panel idlePanel) {
				return idleScene;
			};

		}.configure(new TheSystem(host, port, panel, outputStream) {
			@Override
			protected void handleMessage(Consumer<MqttMessage> consumer, MqttMessage message) {
				super.handleMessage(consumer, message);
				lock.lock();
				try {
					unconsumedMessages--;
					consumed.signalAll();
				} finally {
					lock.unlock();
				}

			}
		}, panel);
	}

}
