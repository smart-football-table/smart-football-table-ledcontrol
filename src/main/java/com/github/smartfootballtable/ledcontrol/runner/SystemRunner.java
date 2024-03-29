package com.github.smartfootballtable.ledcontrol.runner;

import static com.github.smartfootballtable.ledcontrol.Color.BLACK;
import static com.github.smartfootballtable.ledcontrol.Color.WHITE;
import static com.github.smartfootballtable.ledcontrol.LedControl.FPS.framesPerSecond;
import static com.github.smartfootballtable.ledcontrol.LedControl.MessageWithTopic.TopicIsEqualTo.topicIsEqualTo;
import static com.github.smartfootballtable.ledcontrol.LedControl.MessageWithTopic.TopicStartsWith.topicStartWith;
import static com.github.smartfootballtable.ledcontrol.panel.Panel.OverlayStrategy.transparentOn;
import static com.github.smartfootballtable.ledcontrol.runner.Colors.FUCHSIA;
import static com.github.smartfootballtable.ledcontrol.runner.Colors.GREEN;
import static com.github.smartfootballtable.ledcontrol.runner.Colors.LIGHT_BLUE;
import static com.github.smartfootballtable.ledcontrol.runner.Colors.PINK;
import static com.github.smartfootballtable.ledcontrol.runner.Colors.TURQUOISE;
import static com.github.smartfootballtable.ledcontrol.runner.Colors.VIOLET;
import static com.github.smartfootballtable.ledcontrol.runner.Colors.YELLOW;
import static com.github.smartfootballtable.ledcontrol.runner.SystemRunner.Messages.isBackgroundlight;
import static com.github.smartfootballtable.ledcontrol.runner.SystemRunner.Messages.isForegroundlight;
import static com.github.smartfootballtable.ledcontrol.runner.SystemRunner.Messages.isGameFoul;
import static com.github.smartfootballtable.ledcontrol.runner.SystemRunner.Messages.isGameover;
import static com.github.smartfootballtable.ledcontrol.runner.SystemRunner.Messages.isIdle;
import static com.github.smartfootballtable.ledcontrol.runner.SystemRunner.Messages.isTeamScore;
import static com.github.smartfootballtable.ledcontrol.runner.SystemRunner.Messages.isTeamScored;
import static com.github.smartfootballtable.ledcontrol.runner.args4j.EnvVars.envVarsAndArgs;
import static com.github.smartfootballtable.ledcontrol.scene.FlashScene.FlashConfig.flash;
import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;
import static java.util.Arrays.stream;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.kohsuke.args4j.OptionHandlerFilter.ALL;
import static org.kohsuke.args4j.ParserProperties.defaults;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import com.github.smartfootballtable.ledcontrol.Animator;
import com.github.smartfootballtable.ledcontrol.Color;
import com.github.smartfootballtable.ledcontrol.LedControl;
import com.github.smartfootballtable.ledcontrol.LedControl.DefaultAnimator;
import com.github.smartfootballtable.ledcontrol.LedControl.MessageWithTopic;
import com.github.smartfootballtable.ledcontrol.LedControl.MessageWithTopic.TopicIsEqualTo;
import com.github.smartfootballtable.ledcontrol.LedControl.MessageWithTopic.TopicStartsWith;
import com.github.smartfootballtable.ledcontrol.connection.SerialConnection;
import com.github.smartfootballtable.ledcontrol.mqtt.MqttAdapter;
import com.github.smartfootballtable.ledcontrol.panel.Panel;
import com.github.smartfootballtable.ledcontrol.panel.StackedPanel;
import com.github.smartfootballtable.ledcontrol.scene.FlashScene;
import com.github.smartfootballtable.ledcontrol.scene.FlashScene.FlashConfig;
import com.github.smartfootballtable.ledcontrol.scene.IdleScene;
import com.github.smartfootballtable.ledcontrol.scene.ScoreScene;

public class SystemRunner {

	public static class Messages {
		private Messages() {
			super();
		}

		public static final TopicStartsWith isTeamScore = topicStartWith("team/score/");
		public static final TopicIsEqualTo isTeamScored = topicIsEqualTo("team/scored");
		public static final TopicIsEqualTo isGameFoul = topicIsEqualTo("game/foul");
		public static final TopicIsEqualTo isGameover = topicIsEqualTo("game/gameover");
		public static final TopicIsEqualTo isIdle = topicIsEqualTo("game/idle");
		public static final TopicIsEqualTo isForegroundlight = topicIsEqualTo("leds/foregroundlight/color");
		public static final TopicIsEqualTo isBackgroundlight = topicIsEqualTo("leds/backgroundlight/color");
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

			return ledControl //
					.when(isBackgroundlight).then(m -> fillWithColor(backgroundPanel, colorFromPayload(m))) //
					.when(isForegroundlight).then(m -> fillWithColor(foregrounddPanel, colorFromPayload(m))) //
					.when(isTeamScored).then(m -> teamScored(winnerPanel, animator, parseInt(m.getPayload()))) //
					.when(isTeamScore)
					.then(m -> teamScore(scoreScene, parseInt(isTeamScore.suffix(m.getTopic())),
							parseInt(m.getPayload()))) //
					.when(isGameFoul).then(m -> foulScene.flash(animator)) //
					.when(isGameover).then(m -> gameoverScene(winnerPanel, flashColors(m)).flash(animator)) //
					.when(isIdle).then(m -> idle(idleScene, animator, parseBoolean(m.getPayload())));
		}

		private void teamScored(Panel winnerPanel, Animator animator, int teamId) {
			if (teamId >= 0 && teamId < teamColors.length) {
				teamScoredScene(winnerPanel, teamColors[teamId]).flash(animator);
			}
		}

		private static void teamScore(ScoreScene scene, int teamId, int score) {
			scene.setScore(teamId, score);
		}

		private static void idle(IdleScene idleScene, Animator animator, boolean idle) {
			if (idle) {
				idleScene.startAnimation(animator);
			} else {
				idleScene.stopAnimation().reset();
			}
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

		private static Panel fillWithColor(Panel panel, Color color) {
			return panel.fill(color).repaint();
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

	public static void main(String... args) throws IOException, InterruptedException, MqttException {
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
		LedControl ledControl = new LedControl(panel, connection.getOutputStream(),
				new DefaultAnimator(framesPerSecond(25)));
		try (MqttAdapter mqttAdapter = new MqttAdapter(mqttHost, mqttPort, ledControl)) {
			new Configurator().configure(ledControl, panel);
			waitForever();
		}
	}

	private static void waitForever() throws InterruptedException {
		Thread.currentThread().join();
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
