package ledcontrol.runner;

import static java.awt.Color.BLACK;
import static java.awt.Color.BLUE;
import static java.awt.Color.RED;
import static java.awt.Color.WHITE;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static ledcontrol.TheSystem.MqttMessage.isTopic;

import java.awt.Color;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

import com.google.gson.Gson;

import ledcontrol.TheSystem;
import ledcontrol.TheSystem.MqttMessage;
import ledcontrol.connection.SerialConnection;
import ledcontrol.panel.Panel;
import ledcontrol.panel.StackedPanel;
import ledcontrol.rest.GameoverMessage;
import ledcontrol.rest.IdleMessage;
import ledcontrol.rest.ScoreMessage;
import ledcontrol.scene.FlashScene;
import ledcontrol.scene.IdleScene;
import ledcontrol.scene.ScoreScene;

public class SystemRunner {

	public static void main(String[] args)
			throws InterruptedException, MqttSecurityException, MqttException, IOException {

		SECONDS.sleep(2);

		SerialConnection connection = new SerialConnection("/dev/ttyUSB4", 230400);
		int ledCount = 120;

		StackedPanel panel = new StackedPanel(ledCount, 1);
		panel.createSubPanel().fill(BLACK);

		try (TheSystem theSystem = configure(new TheSystem("localhost", 1883, panel, connection.getOutputStream()),
				panel)) {
			Object o = new Object();
			synchronized (o) {
				o.wait();
			}
		}

	}

	public static TheSystem configure(TheSystem theSystem, StackedPanel panel) {
		Color colorTeam1 = BLUE;
		Color colorTeam2 = RED;

		Panel goalPanel = panel.createSubPanel();
		Panel flashPanel = panel.createSubPanel();
		Panel idlePanel = panel.createSubPanel();

		ScoreScene goalScene = new ScoreScene(goalPanel, colorTeam1, colorTeam2).pixelsPerGoal(1);
		FlashScene flashScene = new FlashScene(flashPanel);
		IdleScene idleScene = new IdleScene(idlePanel);

		Gson gson = new Gson();
		theSystem.whenThen(isTopic("score"), m -> {
			int[] score = parsePayload(gson, m, ScoreMessage.class).score;
			goalScene.setScore(score);
		});
		theSystem.whenThen(isTopic("foul"), flashThenWait(flashScene, WHITE, SECONDS, 1));
		Consumer<MqttMessage> winnerColor = (Consumer<MqttMessage>) m -> flashScene
				.fill(parsePayload(gson, m, GameoverMessage.class).winner == 0 ? colorTeam1 : colorTeam2);
		Consumer<MqttMessage> sleep250Ms = sleep(MILLISECONDS, 250);
		Consumer<? super MqttMessage> clear = m -> flashScene.clear();
		theSystem.whenThen(isTopic("gameover"),
				winnerColor.andThen(sleep250Ms).andThen(clear).andThen(sleep250Ms).andThen(winnerColor)
						.andThen(sleep250Ms).andThen(clear).andThen(sleep250Ms).andThen(winnerColor).andThen(sleep250Ms)
						.andThen(clear));
		theSystem.whenThen(isTopic("idle"), m -> {
			if (parsePayload(gson, m, IdleMessage.class).idle) {
				idleScene.startAnimation(theSystem.getAnimator());
			} else {
				idleScene.stopAnimation();
			}
		});
		return theSystem;
	}

	private static Consumer<MqttMessage> flashThenWait(FlashScene flashScene, Color color, TimeUnit timeUnit,
			int duration) {
		return ((Consumer<MqttMessage>) m -> flashScene.fill(color)).andThen(sleep(timeUnit, duration))
				.andThen(m -> flashScene.clear());
	}

	private static Consumer<MqttMessage> sleep(TimeUnit timeUnit, int duration) {
		return m -> {
			try {
				timeUnit.sleep(duration);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		};
	}

	private static <T> T parsePayload(Gson gson, MqttMessage m, Class<T> clazz) {
		return gson.fromJson(m.getPayload(), clazz);
	}

}
