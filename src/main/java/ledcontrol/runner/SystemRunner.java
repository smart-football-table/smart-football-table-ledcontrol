package ledcontrol.runner;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;
import static java.util.Arrays.stream;
import static java.util.concurrent.TimeUnit.SECONDS;
import static ledcontrol.TheSystem.MessageWithTopic.topicIsEqualTo;
import static ledcontrol.TheSystem.MessageWithTopic.topicStartWith;
import static ledcontrol.panel.Panel.OverlayStrategy.transparentOn;
import static ledcontrol.runner.Colors.FUCHSIA;
import static ledcontrol.runner.Colors.GREEN;
import static ledcontrol.runner.Colors.LIGHT_BLUE;
import static ledcontrol.runner.Colors.PINK;
import static ledcontrol.runner.Colors.TURQUOISE;
import static ledcontrol.runner.Colors.VIOLET;
import static ledcontrol.runner.Colors.YELLOW;
import static ledcontrol.runner.args4j.EnvVars.envVarsAndArgs;
import static ledcontrol.scene.FlashScene.FlashConfig.flash;
import static org.kohsuke.args4j.OptionHandlerFilter.ALL;
import static org.kohsuke.args4j.ParserProperties.defaults;

import java.awt.Color;
import java.io.IOException;
import java.util.Arrays;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import ledcontrol.TheSystem;
import ledcontrol.TheSystem.MessageWithTopic;
import ledcontrol.connection.SerialConnection;
import ledcontrol.mqtt.MqttAdapter;
import ledcontrol.panel.Panel;
import ledcontrol.panel.StackedPanel;
import ledcontrol.scene.FlashScene;
import ledcontrol.scene.IdleScene;
import ledcontrol.scene.ScoreScene;

public class SystemRunner {

	public static class Configurator {

		private final Color[] teamColors;

		public Configurator() {
			this(TURQUOISE, PINK);
		}

		public Configurator(Color... teamColors) {
			this.teamColors = teamColors;
		}

		public TheSystem configure(TheSystem theSystem, StackedPanel panel) {
			Panel backgroundPanel = panel.createSubPanel().fill(BLACK);
			Panel goalPanel = panel.createSubPanel();
			Panel foulPanel = panel.createSubPanel();
			Panel winnerPanel = panel.createSubPanel();
			Panel idlePanel = panel.createSubPanel();
			Panel foregrounddPanel = panel.createSubPanel().fill(BLACK).overlayStrategy(transparentOn(BLACK));

			ScoreScene scoreScene = scoreScene(goalPanel);
			IdleScene idleScene = idleScene(idlePanel);

			return //
			theSystem //
					.when(topicIsEqualTo("leds/backgroundlight/color"))
					.then(m -> backgroundPanel.fill(colorFromPayload(m)).repaint()) //
					.when(topicIsEqualTo("leds/foregroundlight/color")) //
					.then(m -> foregrounddPanel.fill(colorFromPayload(m)).repaint()) //
					.when(topicIsEqualTo("team/scored")) //
					.then(m -> {
						int idx = parseInt(m.getPayload());
						if (idx >= 0 && idx < teamColors.length) {
							Color color = teamColors[idx];
							FlashScene fc = new FlashScene(winnerPanel, //
									flash(color, 24), flash(BLACK, 24), //
									flash(color, 24), flash(BLACK, 24), //
									flash(color, 24), flash(BLACK, 24));
							fc.flash(theSystem.getAnimator());
						}

					}) //
					.when(topicStartWith("team/score/")) //
					.then(m -> {
						int teamid = parseInt(m.getTopic().substring("team/score/".length()));
						scoreScene.setScore(teamid, parseInt(m.getPayload()));
					}) //
					.when(topicIsEqualTo("game/foul")) //
					.then(m -> foulScene(foulPanel).flash(theSystem.getAnimator())) //
					.when(topicIsEqualTo("game/gameover")) //
					.then(m -> {
						Color[] flashColors = getFlashColors(m);
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
					}) //
					.when(topicIsEqualTo("game/idle")) //
					.then(m -> {
						if (parseBoolean(m.getPayload())) {
							idleScene.startAnimation(theSystem.getAnimator());
						} else {
							idleScene.stopAnimation().reset();
						}
					});
		}

		private Color[] getFlashColors(MessageWithTopic message) {
			int[] winners = Arrays.stream(message.getPayload().split("\\,")).mapToInt(Integer::parseInt).toArray();
			if (winners.length > 1) {
				return teamColors;
			}
			return contains(winners, 0) ? new Color[] { teamColors[0], teamColors[0] }
					: new Color[] { teamColors[1], teamColors[1] };
		}

		private boolean contains(int[] winners, int team) {
			return stream(winners).anyMatch(i -> i == team);
		}

		private static Color colorFromPayload(MessageWithTopic message) {
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

	@Option(name = "-tty", usage = "tty device to use for serial communication. "
			+ "If omitted the first available port is used")
	String tty;
	@Option(name = "-baudrate", usage = "baudrate for the tty device")
	int baudrate = 230400;
	@Option(name = "-leds", required = true, usage = "how many leds are connected to the TPM2 device")
	int leds;
	@Option(name = "-mqttHost", usage = "hostname of the mqtt broker")
	String mqttHost = "localhost";
	@Option(name = "-mqttPort", usage = "port of the mqtt broker")
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
		TheSystem system = new TheSystem(panel, connection.getOutputStream());
		try (MqttAdapter mqttAdapter = new MqttAdapter(mqttHost, mqttPort, system)) {
			new Configurator().configure(system, panel);
			Object o = new Object();
			synchronized (o) {
				o.wait();
			}
		}
	}

	boolean parseArgs(String... args) {
		CmdLineParser parser = new CmdLineParser(this, defaults().withUsageWidth(80));
		try {
			parser.parseArgument(envVarsAndArgs(parser, args));
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
		System.err.println("  Example: java " + mainClassName + parser.printExample(ALL));
	}

}
