package actors;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import events.AnimationEnded;
import events.CardClicked;
import events.EndTurnClicked;
import events.EventProcessor;
import events.Heartbeat;
import events.Initalize;
import events.OtherClicked;
import events.TileClicked;
import events.UnitMoving;
import events.UnitStopped;
import play.libs.Json;
import structures.GameState;
import utils.ImageListForPreLoad;
import play.libs.Json;

/**
 * The game actor is an Akka Actor that receives events from the user front-end UI.
 *
 * @author Dr. Richard McCreadie
 */
public class GameActor extends AbstractActor {

	private ObjectMapper mapper = new ObjectMapper();
	private ActorRef out;
	private Map<String,EventProcessor> eventProcessors;
	private GameState gameState;

	@SuppressWarnings("deprecation")
	public GameActor(ActorRef out) {

		this.out = out;

		eventProcessors = new HashMap<String,EventProcessor>();

		eventProcessors.put("initialize", new Initalize());
		eventProcessors.put("initalize", new Initalize());

		eventProcessors.put("heartbeat", new Heartbeat());
		eventProcessors.put("unitMoving", new UnitMoving());
		eventProcessors.put("unitstopped", new UnitStopped());
		eventProcessors.put("tileclicked", new TileClicked());
		eventProcessors.put("cardclicked", new CardClicked());
		eventProcessors.put("endturnclicked", new EndTurnClicked());
		eventProcessors.put("otherclicked", new OtherClicked());

		// NEW: UI ack for animation/action completion
		eventProcessors.put("animationended", new AnimationEnded());
		// be tolerant to alternative casing used by the UI
		eventProcessors.put("animationEnded", new AnimationEnded());
		eventProcessors.put("AnimationEnded", new AnimationEnded());

		gameState = new GameState();

		Set<String> images = ImageListForPreLoad.getImageListForPreLoad();

		try {
			ObjectNode readyMessage = Json.newObject();
			readyMessage.put("messagetype", "actorReady");
			readyMessage.put("preloadImages", mapper.readTree(mapper.writeValueAsString(images)));
			out.tell(readyMessage, out);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Receive createReceive() {
		return receiveBuilder()
				.match(JsonNode.class, message -> {
					System.out.println(message);
					processMessage(message.get("messagetype").asText(), message);
				}).build();
	}

	@SuppressWarnings({"deprecation"})
	public void processMessage(String messageType, JsonNode message) throws Exception{

		EventProcessor processor = eventProcessors.get(messageType);
		if (processor==null) {
			System.err.println("GameActor: Recieved unknown event type "+messageType);
		} else {
			processor.processEvent(out, gameState, message);
		}
	}

	public void reportError(String errorText) {
		ObjectNode returnMessage = Json.newObject();
		returnMessage.put("messagetype", "ERR");
		returnMessage.put("error", errorText);
		out.tell(returnMessage, out);
	}
}