package ledcontrol;

import static au.com.dius.pact.consumer.junit5.ProviderType.ASYNCH;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

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
import ledcontrol.runner.Colors;
import ledcontrol.runner.SystemRunner.Configurator;
import ledcontrol.scene.IdleScene;
import ledcontrol.scene.ScoreScene;

@ExtendWith(PactConsumerTestExt.class)
class ContractTests {

	private static final Color COLOR_TEAM_LEFT = Colors.BLUE;
	private static final Color COLOR_TEAM_RIGHT = Colors.ORANGE;

	private final IdleScene idleScene = mock(IdleScene.class);
	private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	private final StackedPanel panel = new StackedPanel(5, 2);

	private LedControl ledControl;
	private List<MessageWithTopic> messagesHandled;

	@BeforeEach
	void setup() throws IOException, MqttException {
		System.setProperty("pact.rootDir", "pacts");
	}

	@Test
	@PactTestFor(providerName = "cognition", pactMethod = "teamLeftScoresPact", providerType = ASYNCH)
	void verifyTeamLeftScores(MessagePact pact) throws InterruptedException, IOException {
		givenTheSystem();
		whenMessagesIsReceived(pact.getMessages());
		assertMessageWasHandled("team/score/0");
	}

	@Pact(consumer = "ledcontrol")
	MessagePact teamLeftScoresPact(MessagePactBuilder builder) {
		return builder //
				.given("a goal was shot") //
				.expectsToReceive("the team's new score") //
				.withContent(new PactDslJsonBody() //
						.stringMatcher("topic", "team\\/score\\/\\d+", "team/score/0") //
						.stringMatcher("payload", "\\d+", "1")) //
				.toPact();
	}

	@Test
	@PactTestFor(providerName = "cognition", pactMethod = "flashesOnFoulPact", providerType = ASYNCH)
	void flashesOnFoul(MessagePact pact)
			throws MqttSecurityException, MqttException, InterruptedException, IOException {
		givenTheSystem();
		whenMessagesIsReceived(pact.getMessages());
		assertMessageWasHandled("game/foul");
	}

	private void assertMessageWasHandled(String topic) throws IOException {
		assertThat(messagesHandled.stream().map(m -> m.getTopic()).collect(toList()), is(Arrays.asList(topic)));
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
		messagesHandled = new ArrayList<>();
		ledControl = new Configurator(COLOR_TEAM_LEFT, COLOR_TEAM_RIGHT) {

			protected ScoreScene scoreScene(Panel goalPanel) {
				return super.scoreScene(goalPanel).pixelsPerGoal(1).spaceDots(0);
			}

			protected ledcontrol.scene.IdleScene idleScene(ledcontrol.panel.Panel idlePanel) {
				return idleScene;
			};

		}.configure(new LedControl(panel, outputStream) {
			@Override
			protected void handleMessage(Consumer<MessageWithTopic> consumer, MessageWithTopic message) {
				super.handleMessage(consumer, message);
				messagesHandled.add(message);
			}
		}, panel);
	}

}
