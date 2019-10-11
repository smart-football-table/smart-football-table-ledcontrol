package ledcontrol;

import static au.com.dius.pact.consumer.junit5.ProviderType.ASYNCH;
import static ledcontrol.runner.Colors.BLUE;
import static ledcontrol.runner.Colors.ORANGE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
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
class ContractTests {

	private static final Color COLOR_TEAM_LEFT = BLUE;
	private static final Color COLOR_TEAM_RIGHT = ORANGE;

	private ScoreScene scoreScene = mock(ScoreScene.class);
	private FlashScene foulScene = mock(FlashScene.class);
	private IdleScene idleScene = mock(IdleScene.class);

	private final StackedPanel panel = new StackedPanel(5, 2);
	private final Animator animator = mock(Animator.class);
	private LedControl ledControl;

	@BeforeEach
	void setup() throws IOException, MqttException {
		System.setProperty("pact.rootDir", "pacts");
	}

	@Test
	@PactTestFor(providerName = "cognition", pactMethod = "teamLeftScorePact", providerType = ASYNCH)
	void verifyTeamLeftScores(MessagePact pact) throws InterruptedException, IOException {
		givenTheSystem();
		whenMessagesIsReceived(pact.getMessages());
		verify(scoreScene, timeout(250)).setScore(1, 2);
	}

	@Pact(consumer = "ledcontrol")
	MessagePact teamLeftScorePact(MessagePactBuilder builder) {
		return builder //
				.given("a goal was shot") //
				.expectsToReceive("the team's new score") //
				.withContent(new PactDslJsonBody() //
						.stringMatcher("topic", "team\\/score\\/\\d+", "team/score/1") //
						.stringMatcher("payload", "\\d+", "2")) //
				.toPact();
	}

	@Test
	@PactTestFor(providerName = "cognition", pactMethod = "flashesOnFoulPact", providerType = ASYNCH)
	void flashesOnFoul(MessagePact pact)
			throws MqttSecurityException, MqttException, InterruptedException, IOException {
		givenTheSystem();
		whenMessagesIsReceived(pact.getMessages());
		verify(foulScene, timeout(250)).flash(animator);
	}

	@Pact(consumer = "ledcontrol")
	MessagePact flashesOnFoulPact(MessagePactBuilder builder) {
		return builder //
				.given("a team fouled") //
				.expectsToReceive("the foul message") //
				.withContent(new PactDslJsonBody() //
						.stringType("topic", "game/foul") //
						.stringMatcher("payload", ".*", "")) //
				.toPact();
	}

	@Test
	@PactTestFor(providerName = "cognition", pactMethod = "idlePact", providerType = ASYNCH)
	void idle(MessagePact pact) throws MqttSecurityException, MqttException, InterruptedException, IOException {
		givenTheSystem();
		whenMessagesIsReceived(pact.getMessages());
		verify(idleScene, timeout(250)).startAnimation(animator);
	}

	@Pact(consumer = "ledcontrol")
	MessagePact idlePact(MessagePactBuilder builder) {
		return builder //
				.given("the table is idle") //
				.expectsToReceive("the idle message") //
				.withContent(new PactDslJsonBody() //
						.stringType("topic", "game/idle") //
						.stringValue("payload", "true")) //
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
		ledControl = new Configurator(COLOR_TEAM_LEFT, COLOR_TEAM_RIGHT) {

			@Override
			protected ScoreScene scoreScene(Panel scorePanel) {
				return scoreScene;
			}

			protected FlashScene foulScene(Panel panel) {
				return foulScene;
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
