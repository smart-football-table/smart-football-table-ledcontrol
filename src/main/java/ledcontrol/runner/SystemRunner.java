package ledcontrol.runner;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;
import static java.util.Arrays.stream;
import static java.util.concurrent.TimeUnit.SECONDS;
import static ledcontrol.LedControl.MessageWithTopic.topicIsEqualTo;
import static ledcontrol.LedControl.MessageWithTopic.topicStartWith;
import static ledcontrol.panel.Panel.OverlayStrategy.transparentOn;
import static ledcontrol.runner.Colors.FUCHSIA;
import static ledcontrol.runner.Colors.GREEN;
import static ledcontrol.runner.Colors.LIGHT_BLUE;
import static ledcontrol.runner.Colors.PINK;
import static ledcontrol.runner.Colors.TURQUOISE;
import static ledcontrol.runner.Colors.VIOLET;
import static ledcontrol.runner.Colors.YELLOW;
import static ledcontrol.runner.SystemRunner.Messages.isBackgroundlight;
import static ledcontrol.runner.SystemRunner.Messages.isForegroundlight;
import static ledcontrol.runner.SystemRunner.Messages.isGameFoul;
import static ledcontrol.runner.SystemRunner.Messages.isGameover;
import static ledcontrol.runner.SystemRunner.Messages.isIdle;
import static ledcontrol.runner.SystemRunner.Messages.isTeamScore;
import static ledcontrol.runner.SystemRunner.Messages.isTeamScored;
import static ledcontrol.runner.args4j.EnvVars.envVarsAndArgs;
import static ledcontrol.scene.FlashScene.FlashConfig.flash;
import static org.kohsuke.args4j.OptionHandlerFilter.ALL;
import static org.kohsuke.args4j.ParserProperties.defaults;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import ledcontrol.Animator;
import ledcontrol.LedControl;
import ledcontrol.LedControl.MessageWithTopic;
import ledcontrol.connection.SerialConnection;
import ledcontrol.mqtt.MqttAdapter;
import ledcontrol.panel.Panel;
import ledcontrol.panel.StackedPanel;
import ledcontrol.scene.FlashScene;
import ledcontrol.scene.FlashScene.FlashConfig;
import ledcontrol.scene.IdleScene;
import ledcontrol.scene.ScoreScene;

public class SystemRunner {

	public static class Messages {
		private Messages() {
			super();
		}
		public static final Predicate<MessageWithTopic> isTeamScore = topicStartWith("team/score/");
		public static final Predicate<MessageWithTopic> isTeamScored = topicIsEqualTo("team/scored");
		public static final Predicate<MessageWithTopic> isGameFoul = topicIsEqualTo("game/foul");
		public static final Predicate<MessageWithTopic> isGameover = topicIsEqualTo("game/gameover");
		public static final Predicate<MessageWithTopic> isIdle = topicIsEqualTo("game/idle");
		public static final Predicate<MessageWithTopic> isForegroundlight = topicIsEqualTo(
				"leds/foregroundlight/color");
		public static final Predicate<MessageWithTopic> isBackgroundlight = topicIsEqualTo(
				"leds/backgroundlight/color");
	}

	public static class Configurator {

		private final Color[] teamColors;

		public Configurator() {
			this(TURQUOISE, PINK);
		}

		public Configurator(Color... teamColors) {
			this.teamColors = teamColors;
		}

		public LedControl configure(LedControl ledControl, StackedPanel panel) {
			Panel backgroundPanel = panel.createSubPanel().fill(BLACK);
			Panel scorePanel = panel.createSubPanel();
			Panel foulPanel = panel.createSubPanel();
			Panel winnerPanel = panel.createSubPanel();
			Panel idlePanel = panel.createSubPanel();
			Panel foregrounddPanel = panel.createSubPanel().fill(BLACK).overlayStrategy(transparentOn(BLACK));

			ScoreScene scoreScene = scoreScene(scorePanel);
			FlashScene foulScene = foulScene(foulPanel);
			IdleScene idleScene = idleScene(idlePanel);

			Animator animator = ledControl.getAnimator();

			return //
			ledControl //
					.when(isBackgroundlight).then(m -> fillWithPayloadColor(backgroundPanel, m)) //
					.when(isForegroundlight).then(m -> fillWithPayloadColor(foregrounddPanel, m)) //
					.when(isTeamScored).then(m -> {
						int idx = parseInt(m.getPayload());
						if (idx >= 0 && idx < teamColors.length) {
							teamScoredScene(winnerPanel, teamColors[idx]).flash(animator);
						}

					}) //
					.when(isTeamScore).then(m -> {
						scoreScene.setScore(parseInt(m.getTopic().substring("team/score/".length())),
								parseInt(m.getPayload()));
					}) //
					.when(isGameFoul).then(m -> foulScene.flash(animator)) //
					.when(isGameover).then(m -> gameoverScene(winnerPanel, flashColors(m)).flash(animator)) //
					.when(isIdle).then(m -> {
						if (parseBoolean(m.getPayload())) {
							idleScene.startAnimation(animator);
						} else {
							idleScene.stopAnimation().reset();
						}
					});
		}

		protected FlashScene teamScoredScene(Panel panel, Color color) {
			return new FlashScene(panel, //
					flash(color, 24), flash(BLACK, 24), //
					flash(color, 24), flash(BLACK, 24), //
					flash(color, 24), flash(BLACK, 24));
		}

		protected ScoreScene scoreScene(Panel panel) {
			return new ScoreScene(panel, teamColors[0], teamColors[1]).pixelsPerGoal(5).spaceDots(1);
		}

		protected FlashScene foulScene(Panel panel) {
			return new FlashScene(panel, //
					flash(WHITE, 6), flash(BLACK, 6), //
					flash(WHITE, 6), flash(BLACK, 6), //
					flash(WHITE, 6), flash(BLACK, 6) //
			);
		}

		protected IdleScene idleScene(Panel panel) {
			return new IdleScene(panel, BLACK, teamColors[0], teamColors[1], LIGHT_BLUE, FUCHSIA, YELLOW, TURQUOISE,
					VIOLET, GREEN, PINK, WHITE);
		}

		protected FlashScene gameoverScene(Panel panel, Color... colors) {
			List<FlashConfig> configs = new ArrayList<>();
			for (int i = 0; i < 2 / colors.length; i++) {
				for (Color color : colors) {
					configs.add(flash(color, 24));
					configs.add(flash(BLACK, 24));
				}
			}
			for (int i = 0; i < 2 / colors.length; i++) {
				for (Color color : colors) {
					configs.add(flash(color, 6));
					configs.add(flash(BLACK, 6));
				}
			}
			assert configs.size() == 8;
			return new FlashScene(panel, configs.toArray(new FlashConfig[configs.size()]));
		}

		private Panel fillWithPayloadColor(Panel panel, MessageWithTopic message) {
			return panel.fill(colorFromPayload(message)).repaint();
		}

		private static Color colorFromPayload(MessageWithTopic message) {
			return Color.decode(message.getPayload());
		}

		private Color[] flashColors(MessageWithTopic message) {
			return stream(message.getPayload().split("\\,")) //
					.map(Integer::parseInt) //
					.filter(i -> i < teamColors.length) //
					.map(i -> teamColors[i]) //
					.toArray(Color[]::new);
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
			throws IOException, InterruptedException, MqttException {
		new SystemRunner().doMain(args);
	}

	void doMain(String[] args) throws InterruptedException, MqttException, IOException {
		if (parseArgs(args)) {
			runSystem();
		}
	}

	private void runSystem() throws IOException, InterruptedException, MqttException {
		SerialConnection connection = new SerialConnection(tty, baudrate);
		SECONDS.sleep(2);
		StackedPanel panel = new StackedPanel(leds, 1);
		LedControl ledControl = new LedControl(panel, connection.getOutputStream());
		try (MqttAdapter mqttAdapter = new MqttAdapter(mqttHost, mqttPort, ledControl)) {
			new Configurator().configure(ledControl, panel);
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
