package ledcontrol;

import static au.com.dius.pact.consumer.junit5.ProviderType.ASYNCH;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.map.HashedMap;
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
import ledcontrol.LedControl.ChainElement;
import ledcontrol.LedControl.MessageWithTopic;
import ledcontrol.panel.StackedPanel;
import ledcontrol.runner.Colors;
import ledcontrol.runner.SystemRunner.Configurator;
import ledcontrol.runner.SystemRunner.Configurator.Foul;
import ledcontrol.runner.SystemRunner.Configurator.Idle;
import ledcontrol.runner.SystemRunner.Configurator.Score;

@ExtendWith(PactConsumerTestExt.class)
class ContractTests {

	private static final Color COLOR_TEAM_LEFT = Colors.BLUE;
	private static final Color COLOR_TEAM_RIGHT = Colors.ORANGE;

	private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	private final StackedPanel panel = new StackedPanel(5, 2);

	private LedControl ledControl;

	private final Map<Class<? extends ChainElement>, ChainElement> spies = new HashedMap<>();

	@BeforeEach
	void setup() throws IOException, MqttException {
		System.setProperty("pact.rootDir", "pacts");
	}

	@Test
	@PactTestFor(providerName = "cognition", pactMethod = "teamLeftScoresPact", providerType = ASYNCH)
	void verifyTeamLeftScores(MessagePact pact) throws InterruptedException, IOException {
		givenTheSystem();
		whenMessagesIsReceived(pact.getMessages());
		assertWasHandled(Score.class);
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
		assertWasHandled(Foul.class);
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
		assertWasHandled(Idle.class);
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

	private void assertWasHandled(Class<? extends ChainElement> clazz) {
		verify(spies.get(clazz)).handle(any(MessageWithTopic.class), any(LedControl.class));
	}

	private void whenMessageIsReceived(Message message) throws InterruptedException {
		ledControl.accept(toMessage(message));
	}

	private MessageWithTopic toMessage(Message message) {
		JSONObject jsonObject = new JSONObject(message.contentsAsString());
		return new MessageWithTopic(jsonObject.getString("topic"), jsonObject.getString("payload"));
	}

	private void givenTheSystem() {
		ledControl = new Configurator(COLOR_TEAM_LEFT, COLOR_TEAM_RIGHT).configure(new LedControl(panel, outputStream) {
			@Override
			public LedControl add(ChainElement element) {
				ChainElement spy = spy(element);
				spies.put(element.getClass(), spy);
				return super.add(spy);
			}
		}, panel);
	}

}
