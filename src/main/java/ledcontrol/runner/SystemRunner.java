package ledcontrol.runner;

import static java.awt.Color.BLACK;
import static java.awt.Color.BLUE;
import static java.awt.Color.RED;
import static java.awt.Color.WHITE;
import static java.util.Arrays.stream;
import static java.util.concurrent.TimeUnit.SECONDS;
import static ledcontrol.TheSystem.MqttMessage.isTopic;
import static ledcontrol.scene.FlashScene.FlashConfig.flash;
import static org.kohsuke.args4j.OptionHandlerFilter.ALL;
import static org.kohsuke.args4j.ParserProperties.defaults;

import java.awt.Color;
import java.io.IOException;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import com.google.gson.Gson;

import ledcontrol.TheSystem;
import ledcontrol.TheSystem.MqttMessage;
import ledcontrol.connection.SerialConnection;
import ledcontrol.panel.Panel;
import ledcontrol.panel.StackedPanel;
import ledcontrol.rest.GameoverMessage;
import ledcontrol.rest.IdleMessage;
import ledcontrol.rest.ScoreMessage;
import ledcontrol.runner.args4j.EnvVarSupportingCmdLineParser;
import ledcontrol.scene.FlashScene;
import ledcontrol.scene.IdleScene;
import ledcontrol.scene.ScoreScene;

public class SystemRunner {

	public static class Configurator {

		private Color colorTeam1 = BLUE;
		private Color colorTeam2 = RED;

		public TheSystem configure(TheSystem theSystem, StackedPanel panel) {
			new SystemRunner.Configurator();

			Panel goalPanel = panel.createSubPanel();
			Panel foulPanel = panel.createSubPanel();
			Panel winnerPanel = panel.createSubPanel();
			Panel idlePanel = panel.createSubPanel();

			ScoreScene goalScene = goalScene(goalPanel);
			FlashScene foulScene = foulScene(foulPanel);
			IdleScene idleScene = idleScene(idlePanel);

			Gson gson = new Gson();
			theSystem.whenThen(isTopic("score"), m -> {
				int[] score = SystemRunner.parsePayload(gson, m, ScoreMessage.class).score;
				goalScene.setScore(score);
			});
			theSystem.whenThen(isTopic("foul"), m -> foulScene.flash(theSystem.getAnimator()));
			theSystem.whenThen(isTopic("gameover"), m -> {
				// TODO handle draws
				Color flashColor = stream(SystemRunner.parsePayload(gson, m, GameoverMessage.class).winners)
						.anyMatch(i -> i == 0) ? colorTeam1 : colorTeam2;
				FlashScene winnerScene = new FlashScene(winnerPanel, //
						flash(flashColor, 24), flash(BLACK, 24), //
						flash(flashColor, 24), flash(BLACK, 24), //
						flash(flashColor, 24), flash(BLACK, 24), // l
						flash(flashColor, 6), flash(BLACK, 6), //
						flash(flashColor, 6), flash(BLACK, 6), //
						flash(flashColor, 6), flash(BLACK, 6));
				winnerScene.flash(theSystem.getAnimator());
			});
			theSystem.whenThen(isTopic("idle"), m -> {
				if (SystemRunner.parsePayload(gson, m, IdleMessage.class).idle) {
					idleScene.startAnimation(theSystem.getAnimator());
				} else {
					idleScene.stopAnimation().reset();
				}
			});
			return theSystem;
		}

		protected IdleScene idleScene(Panel idlePanel) {
			return new IdleScene(idlePanel, BLACK, colorTeam1, colorTeam2);
		}

		protected FlashScene foulScene(Panel foulPanel) {
			return new FlashScene(foulPanel, flash(WHITE, 6), flash(BLACK, 6), flash(WHITE, 6), flash(BLACK, 6),
					flash(WHITE, 6), flash(BLACK, 6));
		}

		protected ScoreScene goalScene(Panel goalPanel) {
			return new ScoreScene(goalPanel, colorTeam1, colorTeam2).pixelsPerGoal(1);
		}

	}

	@Option(name = "-tty", metaVar = "TTY")
	String tty = "/dev/ttyUSB0";
	@Option(name = "-baudrate", metaVar = "BAUDRATE")
	int baudrate = 230400;
	@Option(name = "-leds", metaVar = "LEDS", required = true)
	int leds;
	@Option(name = "-mqttHost", metaVar = "MQTTHOST")
	String mqttHost = "localhost";
	@Option(name = "-mqttPort", metaVar = "MQTTPORT")
	int mqttPort = 1883;

	public static void main(String... args)
			throws IOException, InterruptedException, MqttSecurityException, MqttException {
		new SystemRunner().doMain(args);
	}

	void doMain(String[] args) throws InterruptedException, MqttSecurityException, MqttException, IOException {
		if (parseArgs(args)) {
			runSystem();
		}
	}

	private void runSystem() throws IOException, InterruptedException, MqttSecurityException, MqttException {
		SerialConnection connection = new SerialConnection(tty, baudrate);
		SECONDS.sleep(2);
		StackedPanel panel = new StackedPanel(leds, 1);
		panel.createSubPanel().fill(BLACK);

		try (TheSystem theSystem = new Configurator()
				.configure(new TheSystem(mqttHost, mqttPort, panel, connection.getOutputStream()), panel)) {
			Object o = new Object();
			synchronized (o) {
				o.wait();
			}
		}
	}

	boolean parseArgs(String... args) {
		CmdLineParser parser = new EnvVarSupportingCmdLineParser(this, defaults().withUsageWidth(80));
		try {
			parser.parseArgument(args);
			return true;
		} catch (CmdLineException e) {
			String mainClassName = getClass().getName();
			System.err.println(e.getMessage());
			System.err.println("java " + mainClassName + " [options...] arguments...");
			parser.printUsage(System.err);
			System.err.println();
			System.err.println("  Example: java " + getClass().getName() + parser.printExample(ALL));
			return false;
		}
	}

	private static <T> T parsePayload(Gson gson, MqttMessage m, Class<T> clazz) {
		return gson.fromJson(m.getPayload(), clazz);
	}

}
