package events;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import structures.GameState;

/**
 * Animation ended acknowledgement from UI.
 *
 * Expected JSON:
 * {
 *   "messagetype": "animationended",
 *   "tag": "AI_MOVE"   // or any tag you locked with AnimationGate.lock(tag)
 * }
 */
public class AnimationEnded implements EventProcessor {

	@Override
	public void processEvent(ActorRef out, GameState gameState, JsonNode message) {
		if (gameState == null) return;
		if (gameState.animationGate == null) return;
		if (message == null || message.get("tag") == null) return;

		String tag = message.get("tag").asText();
		gameState.animationGate.unlock(tag);

		// When all locks cleared, allow next actions
		if (!gameState.animationGate.isLocked()) {
			gameState.inputLocked = false;
		}
	}

}