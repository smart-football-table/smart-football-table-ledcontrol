package ledcontrol.runner;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static java.util.Arrays.stream;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Stream.concat;
import static ledcontrol.TheSystem.MqttMessage.isTopic;
import static ledcontrol.runner.args4j.EnvVars.readEnvVars;
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
import ledcontrol.scene.FlashScene;
import ledcontrol.scene.IdleScene;
import ledcontrol.scene.ScoreScene;

public class SystemRunner {

	public static class Configurator {

		private Color colorTeam1 = Color.decode("#0066b3"); // BLUE
		private Color colorTeam2 = Color.decode("#ff6600"); // ORANGE

		public TheSystem configure(TheSystem theSystem, StackedPanel panel) {
			Panel backgroundPanel = panel.createSubPanel();
			Panel goalPanel = panel.createSubPanel();
			Panel foulPanel = panel.createSubPanel();
			Panel winnerPanel = panel.createSubPanel();
			Panel idlePanel = panel.createSubPanel();
			Panel foregrounddPanel = panel.createSubPanel();

			backgroundPanel.fill(BLACK);

			ScoreScene goalScene = goalScene(goalPanel);
			IdleScene idleScene = idleScene(idlePanel);

			Gson gson = new Gson();
			theSystem.whenThen(isTopic("backgroundlight"), m -> {
				String color = m.getPayload();
				backgroundPanel.fill(Color.decode(color));
				backgroundPanel.repaint();
			});
			theSystem.whenThen(isTopic("foregroundlight"), m -> {
				String color = m.getPayload();
				foregrounddPanel.fill(Color.decode(color));
				foregrounddPanel.repaint();
			});
			theSystem.whenThen(isTopic("score"), m -> {
				int[] score = parsePayload(gson, m, ScoreMessage.class).score;
				goalScene.setScore(score);
			});
			theSystem.whenThen(isTopic("foul"), m -> foulScene(foulPanel).flash(theSystem.getAnimator()));
			theSystem.whenThen(isTopic("gameover"), m -> {
				// TODO handle draws
				Color flashColor = stream(parsePayload(gson, m, GameoverMessage.class).winners).anyMatch(i -> i == 0)
						? colorTeam1
						: colorTeam2;
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
				if (parsePayload(gson, m, IdleMessage.class).idle) {
					idleScene.startAnimation(theSystem.getAnimator());
				} else {
					idleScene.stopAnimation().reset();
				}
			});
			return theSystem;
		}

		protected IdleScene idleScene(Panel idlePanel) {
			Color lightBlue = Color.decode("#87d0ea");
			Color fuchsia = Color.decode("#d10a45");
			Color yellow = Color.decode("#facb41");
			Color turquoise = Color.decode("#56bcb1");
			Color violet = Color.decode("#a0509a");
			Color green = Color.decode("#8ac056");
			Color pink = Color.decode("#db4e7a");
			Color white = Color.decode("#ffffff");
			return new IdleScene(idlePanel, BLACK, colorTeam1, colorTeam2, lightBlue, fuchsia, yellow, turquoise,
					violet, green, pink, white);
		}

		protected FlashScene foulScene(Panel foulPanel) {
			return new FlashScene(foulPanel, flash(WHITE, 6), flash(BLACK, 6), flash(WHITE, 6), flash(BLACK, 6),
					flash(WHITE, 6), flash(BLACK, 6));
		}

		protected ScoreScene goalScene(Panel goalPanel) {
			return new ScoreScene(goalPanel, colorTeam1, colorTeam2).pixelsPerGoal(5);
		}

	}

	@Option(name = "-h", help = true)
	boolean help;

	@Option(name = "-tty", metaVar = "TTY", usage = "tty device to use for serial communication. "
			+ "If omitted the first available port is used")
	String tty;
	@Option(name = "-baudrate", metaVar = "BAUDRATE", usage = "baudrate for the tty device")
	int baudrate = 230400;
	@Option(name = "-leds", metaVar = "LEDS", required = true, usage = "how many leds are connected to the TPM2 device")
	int leds;
	@Option(name = "-mqttHost", metaVar = "MQTTHOST", usage = "hostname of the mqtt broker")
	String mqttHost = "localhost";
	@Option(name = "-mqttPort", metaVar = "MQTTPORT", usage = "port of the mqtt broker")
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
		try (TheSystem theSystem = new Configurator()
				.configure(new TheSystem(mqttHost, mqttPort, panel, connection.getOutputStream()), panel)) {
			Object o = new Object();
			synchronized (o) {
				o.wait();
			}
		}
	}

	boolean parseArgs(String... args) {
		CmdLineParser parser = new CmdLineParser(this, defaults().withUsageWidth(80));
		try {
			parser.parseArgument(concat(readEnvVars(parser.getOptions()), stream(args)).toArray(String[]::new));
			if (!help) {
				return true;
			}
			printHelp(parser);
		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			printHelp(parser);
		}
		return false;
	}

	private void printHelp(CmdLineParser parser) {
		String mainClassName = getClass().getName();
		System.err.println("java " + mainClassName + " [options...] arguments...");
		parser.printUsage(System.err);
		System.err.println();
		System.err.println("  Example: java " + getClass().getName() + parser.printExample(ALL));
	}

	private static <T> T parsePayload(Gson gson, MqttMessage m, Class<T> clazz) {
		return gson.fromJson(m.getPayload(), clazz);
	}

}
