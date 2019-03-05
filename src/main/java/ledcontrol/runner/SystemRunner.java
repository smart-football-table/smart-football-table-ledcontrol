package ledcontrol.runner;

import static java.awt.Color.BLACK;
import static java.awt.Color.BLUE;
import static java.awt.Color.RED;
import static java.awt.Color.WHITE;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static ledcontrol.TheSystem.MqttMessage.isTopic;
import static org.kohsuke.args4j.OptionHandlerFilter.ALL;
import static org.kohsuke.args4j.ParserProperties.defaults;

import java.awt.Color;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import com.google.gson.Gson;

import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;
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

	@Option(name = "-tty")
	private String tty = "/dev/ttyUSB0";
	@Option(name = "-baudrate")
	private int baudrate = 230400;
	@Option(name = "-leds", required = true)
	private int leds;
	@Option(name = "-mqttHost")
	private String mqttHost = "localhost";
	@Option(name = "-mqttPort")
	private int mqttPort = 1883;

	public static void main(String[] args) throws IOException, NoSuchPortException, PortInUseException,
			UnsupportedCommOperationException, InterruptedException, MqttSecurityException, MqttException {
		new SystemRunner().doMain(args);
	}

	private void doMain(String[] args) throws InterruptedException, NoSuchPortException, PortInUseException,
			UnsupportedCommOperationException, MqttSecurityException, MqttException, IOException {
		if (parseArgs(this, args)) {
			SerialConnection connection = new SerialConnection(tty, baudrate);
			SECONDS.sleep(2);
			StackedPanel panel = new StackedPanel(leds, 1);
			panel.createSubPanel().fill(BLACK);

			try (TheSystem theSystem = configure(new TheSystem(mqttHost, mqttPort, panel, connection.getOutputStream()),
					panel)) {
				Object o = new Object();
				synchronized (o) {
					o.wait();
				}
			}
		}
	}

	private static boolean parseArgs(SystemRunner bean, String[] args) {
		CmdLineParser parser = new CmdLineParser(bean, defaults().withUsageWidth(80));
		try {
			parser.parseArgument(args);
			return true;
		} catch (CmdLineException e) {
			String mainClassName = bean.getClass().getName();
			System.err.println(e.getMessage());
			System.err.println("java " + mainClassName + " [options...] arguments...");
			parser.printUsage(System.err);
			System.err.println();
			System.err.println("  Example: java " + bean.getClass().getName() + parser.printExample(ALL));
			return false;
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
		IdleScene idleScene = new IdleScene(idlePanel, BLACK, colorTeam1, colorTeam2);

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
