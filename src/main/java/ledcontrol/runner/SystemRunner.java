package ledcontrol.runner;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;
import static java.util.Arrays.stream;
import static java.util.concurrent.TimeUnit.SECONDS;
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

import ledcontrol.Animator;
import ledcontrol.LedControl;
import ledcontrol.LedControl.ChainElement;
import ledcontrol.LedControl.MessageWithTopic;
import ledcontrol.connection.SerialConnection;
import ledcontrol.mqtt.MqttAdapter;
import ledcontrol.panel.Panel;
import ledcontrol.panel.StackedPanel;
import ledcontrol.scene.FlashScene;
import ledcontrol.scene.IdleScene;
import ledcontrol.scene.ScoreScene;

public class SystemRunner {

	public static class Configurator {

		public static class Background implements ChainElement {

			private final Panel panel;

			private Background(Panel panel) {
				this.panel = panel;
			}

			public boolean handle(MessageWithTopic message, Animator animator) {
				if (message.getTopic().equals("leds/backgroundlight/color")) {
					panel.fill(colorFromPayload(message)).repaint();
					return true;
				}
				return false;
			}
		}

		public static class Foreground implements ChainElement {
			private final Panel panel;

			private Foreground(Panel panel) {
				this.panel = panel;
			}

			@Override
			public boolean handle(MessageWithTopic message, Animator animator) {
				if (message.getTopic().equals("leds/foregroundlight/color")) {
					panel.fill(colorFromPayload(message)).repaint();
					return true;
				}
				return false;
			}
		}

		public static class Scored implements ChainElement {
			private final Panel panel;
			private final Color[] teamColors;

			private Scored(Panel panel, Color teamColors[]) {
				this.panel = panel;
				this.teamColors = teamColors;
			}

			@Override
			public boolean handle(MessageWithTopic message, Animator animator) {
				if (message.getTopic().equals("leds/foregroundlight/color")) {
					int idx = parseInt(message.getPayload());
					if (idx >= 0 && idx < teamColors.length) {
						Color color = teamColors[idx];
						FlashScene scene = new FlashScene(panel, //
								flash(color, 24), flash(BLACK, 24), //
								flash(color, 24), flash(BLACK, 24), //
								flash(color, 24), flash(BLACK, 24));
						scene.flash(animator);
					}
					return true;
				}
				return false;
			}
		}

		public static class Score implements ChainElement {

			private final ScoreScene scoreScene;

			public Score(ScoreScene scoreScene) {
				this.scoreScene = scoreScene;
			}

			public ScoreScene getScoreScene() {
				return scoreScene;
			}

			@Override
			public boolean handle(MessageWithTopic message, Animator animator) {
				if (message.getTopic().startsWith("team/score/")) {
					int teamid = parseInt(message.getTopic().substring("team/score/".length()));
					this.scoreScene.setScore(teamid, parseInt(message.getPayload()));
					return true;
				}
				return false;
			}
		}

		public static class Foul implements ChainElement {

			private final FlashScene scene;

			private Foul(Panel panel) {
				this.scene = new FlashScene(panel, flash(WHITE, 6), flash(BLACK, 6), flash(WHITE, 6), flash(BLACK, 6),
						flash(WHITE, 6), flash(BLACK, 6));
			}

			public boolean handle(MessageWithTopic message, Animator animator) {
				if (message.getTopic().equals("game/foul")) {
					scene.flash(animator);
					return true;
				}
				return false;
			}
		}

		public static class Gameover implements ChainElement {
			private final Panel winnerPanel;
			private final Color[] teamColors;

			private Gameover(Panel winnerPanel, Color teamColors[]) {
				this.winnerPanel = winnerPanel;
				this.teamColors = teamColors;
			}

			@Override
			public boolean handle(MessageWithTopic message, Animator animator) {
				if (message.getTopic().equals("game/gameover")) {
					Color[] flashColors = getFlashColors(message);
					FlashScene scene = new FlashScene(winnerPanel, //
							flash(flashColors[0], 24), flash(BLACK, 24), //
							flash(flashColors[1], 24), flash(BLACK, 24), //
							flash(flashColors[0], 24), flash(BLACK, 24), //
							flash(flashColors[1], 24), flash(BLACK, 24), //
							flash(flashColors[0], 6), flash(BLACK, 6), //
							flash(flashColors[1], 6), flash(BLACK, 6), //
							flash(flashColors[0], 6), flash(BLACK, 6), //
							flash(flashColors[1], 6), flash(BLACK, 6));
					scene.flash(animator);
					return true;
				}
				return false;
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

		}

		public static class Idle implements ChainElement {
			private final IdleScene scene;

			private Idle(Panel panel, IdleScene scene) {
				this.scene = scene;
			}

			@Override
			public boolean handle(MessageWithTopic message, Animator animator) {
				if (message.getTopic().equals("game/idle")) {
					if (parseBoolean(message.getPayload())) {
						scene.startAnimation(animator);
					} else {
						scene.stopAnimation().reset();
					}
					return true;
				}
				return false;
			}
		}

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
			Panel foregroundPanel = panel.createSubPanel().fill(BLACK).overlayStrategy(transparentOn(BLACK));

			return ledControl.addAll(new Background(backgroundPanel), //
					new Foreground(foregroundPanel), //
					new Scored(winnerPanel, teamColors), //
					new Score(new ScoreScene(scorePanel, teamColors[0], teamColors[1]).pixelsPerGoal(5).spaceDots(1)), //
					new Foul(foulPanel), //
					new Gameover(winnerPanel, teamColors), //
					new Idle(idlePanel, idleScene(idlePanel)) //
			);
		}

		@Deprecated
		protected IdleScene idleScene(Panel panel) {
			return new IdleScene(panel, BLACK, teamColors[0], teamColors[1], LIGHT_BLUE, FUCHSIA, YELLOW, TURQUOISE,
					VIOLET, GREEN, PINK, WHITE);
		}

		private static Color colorFromPayload(MessageWithTopic message) {
			return Color.decode(message.getPayload());
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
