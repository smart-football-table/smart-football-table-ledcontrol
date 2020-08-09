package ledcontrol;

import static au.com.dius.pact.consumer.ConsumerPactBuilder.jsonBody;
import static au.com.dius.pact.consumer.junit5.ProviderType.ASYNCH;
import static ledcontrol.ContractTest.PactBuilder.a;
import static ledcontrol.ContractTest.PactBuilder.message;
import static ledcontrol.runner.Colors.BLUE;
import static ledcontrol.runner.Colors.ORANGE;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.messaging.Message;
import au.com.dius.pact.core.model.messaging.MessagePact;
import ledcontrol.LedControl.MessageWithTopic;
import ledcontrol.panel.Panel;
import ledcontrol.panel.StackedPanel;
import ledcontrol.runner.SystemRunner.Configurator;
import ledcontrol.scene.FlashScene;
import ledcontrol.scene.IdleScene;
import ledcontrol.scene.ScoreScene;

@ExtendWith(PactConsumerTestExt.class)
class ContractTest {

	private static final class FlashScene4Test extends FlashScene {
		private Animator animator;
		private final Color[] colors;

		private FlashScene4Test(Panel panel, Color... colors) {
			super(panel);
			this.colors = colors;
		}

		@Override
		public void flash(Animator animator) {
			this.animator = animator;
		}
	}

	static class PactBuilder {

		private PactDslJsonBody jsonBody = jsonBody();

		private PactBuilder withTopic(String value) {
			jsonBody.stringValue("topic", value);
			return this;
		}

		private PactBuilder withPayload(String value) {
			jsonBody.stringValue("payload", value);
			return this;
		}

		private PactBuilder withTopic(String regex, String value) {
			jsonBody.stringMatcher("topic", regex, value);
			return this;
		}

		private PactBuilder withPayload(String regex, String value) {
			jsonBody.stringMatcher("payload", regex, value);
			return this;
		}

		public static PactBuilder message() {
			return new PactBuilder() // .withPayload(".*", "any payload")
			;
		}

		private PactDslJsonBody build() {
			return jsonBody;
		}

		static PactDslJsonBody a(PactBuilder builder) {
			return builder.build();
		}
	}

	private static final String CONSUMER = "ledcontrol";
	private static final String PROVIDER = "cognition";

	private static final Color COLOR_TEAM_LEFT = BLUE;
	private static final Color COLOR_TEAM_RIGHT = ORANGE;

	private ScoreScene scoreScene = mock(ScoreScene.class);
	private FlashScene foulScene = mock(FlashScene.class);
	private FlashScene4Test gameoverScene;
	private IdleScene idleScene = mock(IdleScene.class);

	private final Animator animator = mock(Animator.class);
	private LedControl ledControl;

	@BeforeEach
	void setup() throws IOException, MqttException {
		System.setProperty("pact.rootDir", "pacts");
	}

	@Test
	@PactTestFor(providerName = PROVIDER, pactMethod = "teamLeftScorePact", providerType = ASYNCH)
	void verifyTeamLeftScores(MessagePact pact) throws InterruptedException, IOException {
		givenTheSystem();
		whenMessagesIsReceived(pact.getMessages());
		verify(scoreScene).setScore(1, 2);
	}

	@Pact(consumer = CONSUMER)
	MessagePact teamLeftScorePact(MessagePactBuilder builder) {
		return builder //
				.given("a goal was shot") //
				.expectsToReceive("the team's new score") //
				.withContent(a(message().withTopic("team\\/score\\/\\d+", "team/score/1").withPayload("\\d+", "2")))
				.toPact();
	}

	@Test
	@PactTestFor(providerName = PROVIDER, pactMethod = "flashesOnFoulPact", providerType = ASYNCH)
	void flashesOnFoul(MessagePact pact)
			throws MqttSecurityException, MqttException, InterruptedException, IOException {
		givenTheSystem();
		whenMessagesIsReceived(pact.getMessages());
		verify(foulScene).flash(animator);
	}

	@Pact(consumer = CONSUMER)
	MessagePact flashesOnFoulPact(MessagePactBuilder builder) {
		return builder //
				.given("a team fouled") //
				.expectsToReceive("the foul message") //
				.withContent(a(message().withTopic("game/foul").withPayload(".*", ""))) //
				.toPact();
	}

	@Test
	@PactTestFor(providerName = PROVIDER, pactMethod = "idlePact", providerType = ASYNCH)
	void idle(MessagePact pact) throws MqttSecurityException, MqttException, InterruptedException, IOException {
		givenTheSystem();
		whenMessagesIsReceived(pact.getMessages());
		verify(idleScene).startAnimation(animator);
	}

	@Pact(consumer = CONSUMER)
	MessagePact idlePact(MessagePactBuilder builder) {
		return builder //
				.given("the table is idle") //
				.expectsToReceive("the idle message") //
				.withContent(a(message().withTopic("game/idle").withPayload("true"))) //
				.toPact();
	}

	@Test
	@PactTestFor(providerName = PROVIDER, pactMethod = "gameoverPact", providerType = ASYNCH)
	void gameover(MessagePact pact) throws MqttSecurityException, MqttException, InterruptedException, IOException {
		givenTheSystem();
		whenMessagesIsReceived(pact.getMessages());
		assertThat(gameoverScene.animator, is(animator));
		assertThat(gameoverScene.colors, is(new Color[] { COLOR_TEAM_RIGHT }));
	}

	@Pact(consumer = CONSUMER)
	MessagePact gameoverPact(MessagePactBuilder builder) {
		return builder //
				.given("a team has won the game") //
				.expectsToReceive("the gameover message") //
				.withContent(a(message().withTopic("game/gameover").withPayload("\\d+", "1"))) //
				.toPact();
	}

	@Test
	@PactTestFor(providerName = PROVIDER, pactMethod = "gameoverDrawPact", providerType = ASYNCH)
	void gameoverDraw(MessagePact pact) throws MqttSecurityException, MqttException, InterruptedException, IOException {
		givenTheSystem();
		whenMessagesIsReceived(pact.getMessages());
		assertThat(gameoverScene.animator, is(animator));
		assertThat(gameoverScene.colors, is(new Color[] { COLOR_TEAM_LEFT, COLOR_TEAM_RIGHT }));
	}

	@Pact(consumer = CONSUMER)
	MessagePact gameoverDrawPact(MessagePactBuilder builder) {
		return builder //
				.given("a game ends draw") //
				.expectsToReceive("the gameover message") //
				.withContent(a(message().withTopic("game/gameover").withPayload("\\d+,\\d+(,\\d+)*", "0,1,2,3"))) //
				.toPact();
	}

	private void whenMessagesIsReceived(List<Message> messages) throws InterruptedException {
		for (Message message : messages) {
			whenMessageIsReceived(message);
		}
	}

	private void whenMessageIsReceived(Message message) throws InterruptedException {
		ledControl.accept(toMessage(message));
	}

	private MessageWithTopic toMessage(Message message) {
		JSONObject jsonObject = new JSONObject(message.contentsAsString());
		return new MessageWithTopic(jsonObject.getString("topic"), jsonObject.getString("payload"));
	}

	private void givenTheSystem() {
		StackedPanel panel = new StackedPanel(3, 1);
		ledControl = new Configurator(COLOR_TEAM_LEFT, COLOR_TEAM_RIGHT) {

			@Override
			protected ScoreScene scoreScene(Panel panel) {
				return scoreScene;
			}

			@Override
			protected FlashScene foulScene(Panel panel) {
				return foulScene;
			};

			@Override
			protected FlashScene gameoverScene(Panel panel, Color... colors) {
				gameoverScene = new FlashScene4Test(panel, colors);
				return gameoverScene;
			};

			@Override
			protected IdleScene idleScene(Panel panel) {
				return idleScene;
			}

		}.configure(new LedControl(panel, mock(OutputStream.class)) {
			@Override
			public Animator getAnimator() {
				return animator;
			}
		}, panel);
	}

}
