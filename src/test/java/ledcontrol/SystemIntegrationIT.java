package ledcontrol;

import static io.moquette.BrokerConstants.HOST_PROPERTY_NAME;
import static io.moquette.BrokerConstants.PORT_PROPERTY_NAME;
import static java.awt.Color.BLACK;
import static java.awt.Color.BLUE;
import static java.awt.Color.RED;
import static java.awt.Color.WHITE;
import static ledcontrol.runner.SystemRunner.configure;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
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

import io.moquette.server.Server;
import io.moquette.server.config.MemoryConfig;
import ledcontrol.panel.StackedPanel;

public class SystemIntegrationIT {

	private static final String LOCALHOST = "localhost";
	private static final Color OFF = BLACK;
	private static final Color COLOR_TEAM_LEFT = BLUE;
	private static final Color COLOR_TEAM_RIGHT = RED;

	private static final int BROKER_PORT = 1883;

	private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

	private final StackedPanel panel = new StackedPanel(5, 2);

	private Server server;
	private TheSystem theSystem;
	private IMqttClient secondClient;

	@Before
	public void setup() throws IOException, MqttException {
		server = newMqttServer(LOCALHOST, BROKER_PORT);
		secondClient = newMqttClient(LOCALHOST, BROKER_PORT, "client2");
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
		givenTheSystemConnectedToBroker(LOCALHOST, BROKER_PORT);
		whenMessageIsReceived(LOCALHOST, BROKER_PORT, "score", score(1, 0));
		assertThat(panelColors(), is(new Color[][] { //
				{ COLOR_TEAM_LEFT, OFF, OFF, OFF, OFF }, //
				{ COLOR_TEAM_LEFT, OFF, OFF, OFF, OFF }, //
		}));
	}

	@Test
	public void teamLeftScoresTwice() throws MqttSecurityException, MqttException, InterruptedException, IOException {
		givenTheSystemConnectedToBroker(LOCALHOST, BROKER_PORT);
		whenMessageIsReceived(LOCALHOST, BROKER_PORT, "score", score(1, 0));
		whenMessageIsReceived(LOCALHOST, BROKER_PORT, "score", score(2, 0));
		assertThat(panelColors(), is(new Color[][] { //
				{ COLOR_TEAM_LEFT, COLOR_TEAM_LEFT, OFF, OFF, OFF }, //
				{ COLOR_TEAM_LEFT, COLOR_TEAM_LEFT, OFF, OFF, OFF }, //
		}));
	}

	private String score(int... scores) {
		return "{ \"score\": [ " + IntStream.of(scores).mapToObj(String::valueOf).collect(Collectors.joining(", "))
				+ " ] }";
	}

	@Test
	public void flashesOnFoul() throws MqttSecurityException, MqttException, InterruptedException, IOException {
		givenTheSystemConnectedToBroker(LOCALHOST, BROKER_PORT);
		whenMessageIsReceived(LOCALHOST, BROKER_PORT, "foul", "");
		assertThat(panelColors(), is(new Color[][] { //
				{ WHITE, WHITE, WHITE, WHITE, WHITE }, //
				{ WHITE, WHITE, WHITE, WHITE, WHITE }, //
		}));

	}

	private Color[][] panelColors() throws IOException {
		int height = panel.getHeight();
		int width = panel.getWidth();
		Color[][] colors = new Color[height][width];
		Tpm2Frame frame = lastFrame();
		for (int y = 0; y < height; y++) {
			System.arraycopy(frame.getColors(), y * width, colors[y], 0, width);
		}
		return colors;
	}

	private Tpm2Frame lastFrame() throws IOException {
		InputStream is = new ByteArrayInputStream(outputStream.toByteArray());
		Tpm2Frame frame = null;
		while (is.available() > 0) {
			frame = Tpm2Frame.fromStream(is);
		}
		return frame;
	}

	private void whenMessageIsReceived(String host, int port, String topic, String message)
			throws MqttSecurityException, MqttException, InterruptedException {
		secondClient.publish(topic, new MqttMessage(message.getBytes()));
		// TODO use lock/condition
		TimeUnit.MILLISECONDS.sleep(500);
	}

	private void givenTheSystemConnectedToBroker(String host, int port) throws MqttSecurityException, MqttException {
		theSystem = configure(new TheSystem(host, port, panel, outputStream), panel);
	}

}
