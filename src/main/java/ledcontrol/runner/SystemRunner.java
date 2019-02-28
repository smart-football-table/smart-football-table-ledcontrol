package ledcontrol.runner;

import static java.awt.Color.BLACK;
import static java.awt.Color.GREEN;
import static java.awt.Color.RED;
import static java.awt.Color.WHITE;
import static java.util.concurrent.TimeUnit.SECONDS;
import static ledcontrol.TheSystem.MqttMessage.isTopic;

import java.io.IOException;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

import com.google.gson.Gson;

import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;
import ledcontrol.TheSystem;
import ledcontrol.TheSystem.MqttMessage;
import ledcontrol.connection.SerialConnection;
import ledcontrol.panel.MixPanel;
import ledcontrol.rest.ScoreMessage;
import ledcontrol.scene.FlashScene;
import ledcontrol.scene.ScoreScene;

public class SystemRunner {

	public static void main(String[] args) throws IOException, NoSuchPortException, PortInUseException,
			UnsupportedCommOperationException, InterruptedException, MqttSecurityException, MqttException {

		SECONDS.sleep(2);

		SerialConnection connection = new SerialConnection("/dev/ttyUSB4", 230400);
		int ledCount = 120;

		MixPanel panel = new MixPanel(ledCount, 1);
		panel.createSubPanel().fill(BLACK);

		try (TheSystem theSystem = configure(new TheSystem("localhost", 1883, panel, connection.getOutputStream()),
				panel)) {
			Object o = new Object();
			synchronized (o) {
				o.wait();
			}
		}

	}

	public static TheSystem configure(TheSystem theSystem, MixPanel panel) {
		ScoreScene goalScene = new ScoreScene(panel.createSubPanel(), RED, GREEN);
		FlashScene flashScene = new FlashScene(panel.createSubPanel());
		Gson gson = new Gson();
		theSystem.whenThen(isTopic("score"), m -> goalScene.setScore(parsePayload(gson, m, ScoreMessage.class).score));
		theSystem.whenThen(isTopic("flash"), m -> flashScene.flash(WHITE, SECONDS, 1));
		return theSystem;
	}

	private static <T> T parsePayload(Gson gson, MqttMessage m, Class<T> clazz) {
		return gson.fromJson(m.getPayload(), clazz);
	}

}
