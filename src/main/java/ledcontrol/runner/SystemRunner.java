package ledcontrol.runner;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;
import static java.util.Arrays.stream;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Stream.concat;
import static ledcontrol.TheSystem.MqttMessage.isTopic;
import static ledcontrol.panel.Panel.OverlayStrategy.transparentOn;
import static ledcontrol.runner.Colors.FUCHSIA;
import static ledcontrol.runner.Colors.GREEN;
import static ledcontrol.runner.Colors.LIGHT_BLUE;
import static ledcontrol.runner.Colors.PINK;
import static ledcontrol.runner.Colors.TURQUOISE;
import static ledcontrol.runner.Colors.VIOLET;
import static ledcontrol.runner.Colors.YELLOW;
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
import ledcontrol.rest.ScoreMessage;
import ledcontrol.scene.FlashScene;
import ledcontrol.scene.IdleScene;
import ledcontrol.scene.ScoreScene;

public class SystemRunner {

	public static class Configurator {

		private static final Color[] teamColors = new Color[] { TURQUOISE, PINK };

		public TheSystem configure(TheSystem theSystem, StackedPanel panel) {
			Panel backgroundPanel = panel.createSubPanel().fill(BLACK);
			Panel goalPanel = panel.createSubPanel();
			Panel foulPanel = panel.createSubPanel();
			Panel winnerPanel = panel.createSubPanel();
			Panel idlePanel = panel.createSubPanel();
			Panel foregrounddPanel = panel.createSubPanel().fill(BLACK).overlayStrategy(transparentOn(BLACK));

			ScoreScene goalScene = scoreScene(goalPanel);
			IdleScene idleScene = idleScene(idlePanel);

			Gson gson = new Gson();
			theSystem.whenThen(isTopic("leds/backgroundlight/color"), m -> {
				backgroundPanel.fill(colorFromPayload(m)).repaint();
			});
			theSystem.whenThen(isTopic("leds/foregroundlight/color"), m -> {
				foregrounddPanel.fill(colorFromPayload(m)).repaint();
			});
			theSystem.whenThen(isTopic("team/scored"), m -> {
				int idx = parseInt(m.getPayload());
				if (idx >= 0 && idx < teamColors.length) {
					Color color = teamColors[idx];
					FlashScene fc = new FlashScene(winnerPanel, //
							flash(color, 24), flash(BLACK, 24), //
							flash(color, 24), flash(BLACK, 24), //
							flash(color, 24), flash(BLACK, 24));
					fc.flash(theSystem.getAnimator());
				}

			});
			theSystem.whenThen(isTopic("game/score"), m -> {
				int[] score = parsePayload(gson, m, ScoreMessage.class).score;
				goalScene.setScore(score);
			});
			theSystem.whenThen(isTopic("game/foul"), m -> foulScene(foulPanel).flash(theSystem.getAnimator()));
			theSystem.whenThen(isTopic("game/gameover"), m -> {
				Color[] flashColors = getFlashColors(gson, m);
				FlashScene winnerScene = new FlashScene(winnerPanel, //
						flash(flashColors[0], 24), flash(BLACK, 24), //
						flash(flashColors[1], 24), flash(BLACK, 24), //
						flash(flashColors[0], 24), flash(BLACK, 24), //
						flash(flashColors[1], 24), flash(BLACK, 24), //
						flash(flashColors[0], 6), flash(BLACK, 6), //
						flash(flashColors[1], 6), flash(BLACK, 6), //
						flash(flashColors[0], 6), flash(BLACK, 6), //
						flash(flashColors[1], 6), flash(BLACK, 6));
				winnerScene.flash(theSystem.getAnimator());
			});
			theSystem.whenThen(isTopic("game/idle"), m -> {
				if (parseBoolean(m.getPayload())) {
					idleScene.startAnimation(theSystem.getAnimator());
				} else {
					idleScene.stopAnimation().reset();
				}
			});
			return theSystem;
		}

		private Color[] getFlashColors(Gson gson, MqttMessage m) {
			int[] winners = parsePayload(gson, m, GameoverMessage.class).winners;
			if (winners.length > 1) {
				return teamColors;
			}
			return contains(winners, 0) ? new Color[] { teamColors[0], teamColors[0] }
					: new Color[] { teamColors[1], teamColors[1] };
		}

		private boolean contains(int[] winners, int team) {
			return stream(winners).anyMatch(i -> i == team);
		}

		private static Color colorFromPayload(MqttMessage message) {
			return Color.decode(message.getPayload());
		}

		protected IdleScene idleScene(Panel idlePanel) {
			return new IdleScene(idlePanel, BLACK, teamColors[0], teamColors[1], LIGHT_BLUE, FUCHSIA, YELLOW, TURQUOISE,
					VIOLET, GREEN, PINK, WHITE);
		}

		protected FlashScene foulScene(Panel foulPanel) {
			return new FlashScene(foulPanel, flash(WHITE, 6), flash(BLACK, 6), flash(WHITE, 6), flash(BLACK, 6),
					flash(WHITE, 6), flash(BLACK, 6));
		}

		protected ScoreScene scoreScene(Panel goalPanel) {
			return new ScoreScene(goalPanel, teamColors[0], teamColors[1]).pixelsPerGoal(5).spaceDots(1);
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
