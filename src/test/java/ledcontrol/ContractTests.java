package ledcontrol;

import static au.com.dius.pact.consumer.junit5.ProviderType.ASYNCH;
import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

	@BeforeEach
	void setup() throws IOException, MqttException {
		System.setProperty("pact.rootDir", "pacts");
	}

	@Test
	@PactTestFor(providerName = "cognition", pactMethod = "teamLeftScoresPact", providerType = ASYNCH)
	void verifyTeamLeftScores(MessagePact pact) throws InterruptedException, IOException {
		givenTheSystem();
		whenMessagesIsReceived(pact.getMessages());
		assertThat(lastPanelState(), is(new Color[][] { //
				{ COLOR_TEAM_LEFT, BLACK, BLACK, BLACK, BLACK }, //
				{ COLOR_TEAM_LEFT, BLACK, BLACK, BLACK, BLACK }, //
		}));
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
		TimeUnit.MILLISECONDS.sleep(40);
		assertThat(lastPanelState(), is(new Color[][] { //
				{ WHITE, WHITE, WHITE, WHITE, WHITE }, //
				{ WHITE, WHITE, WHITE, WHITE, WHITE }, //
		}));
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

	private Color[][] lastPanelState() throws IOException {
		return toColors(last(receivedFrames()));
	}

	private Color[][] toColors(Tpm2Frame frame) {
		int height = panel.getHeight();
		int width = panel.getWidth();
		Color[][] colors = new Color[height][width];
		for (int y = 0; y < height; y++) {
			System.arraycopy(frame.getColors(), y * width, colors[y], 0, width);
		}
		return colors;
	}

	private List<Tpm2Frame> receivedFrames() throws IOException {
		InputStream is = new ByteArrayInputStream(outputStream.toByteArray());
		List<Tpm2Frame> frames = new ArrayList<Tpm2Frame>();
		while (is.available() > 0) {
			frames.add(Tpm2Frame.fromStream(is));
		}
		return frames;
	}

	private static <T> T last(List<T> list) throws IOException {
		return list.get(list.size() - 1);
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

			protected ScoreScene scoreScene(Panel goalPanel) {
				return super.scoreScene(goalPanel).pixelsPerGoal(1).spaceDots(0);
			}

			protected ledcontrol.scene.IdleScene idleScene(ledcontrol.panel.Panel idlePanel) {
				return idleScene;
			};

		}.configure(new LedControl(panel, outputStream), panel);
	}

}
