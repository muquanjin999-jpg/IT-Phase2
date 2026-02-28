package events;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import structures.GameState;

/**
 * In the user’s browser, the game is running in an infinite loop, where there is around a 1 second delay
 * between each loop. Its during each loop that the UI acts on the commands that have been sent to it. A
 * heartbeat event is fired at the end of each loop iteration. As with all events this is received by the Game
 * Actor, which you can use to trigger game logic.
 *
 * {
 *   String messageType = "heartbeat"
 * }
 *
 * This implementation drives the AI turn-loop:
 * P1 EndTurn -> set aiTurnActive=true -> Heartbeat steps AI actions -> AI EndTurn -> back to P1
 *
 * @author Dr. Richard McCreadie
 */
public class Heartbeat implements EventProcessor {

	@Override
	public void processEvent(ActorRef out, GameState gameState, JsonNode message) {

		// Basic guards
		if (gameState == null) return;
		if (!gameState.gameInitalised) return;
		if (gameState.domainGameManager == null || gameState.domainState == null) return;
		if (gameState.aiController == null) return;
		if (gameState.domainState.isGameOver()) return;

		// Cooldown ticks (used for non-move actions where template has no "stopped" ack)
		if (gameState.aiCooldownTicks > 0) {
			gameState.aiCooldownTicks--;
			return;
		}

		// Only run when AI loop is enabled AND it is P2's turn
		if (!gameState.aiTurnActive) return;
		if (!gameState.domainState.getActivePlayerId().equals(game.model.GameState.P2)) return;

		// If UI is locked (e.g., moving animation in progress), wait
		if (gameState.inputLocked) return;

		// Per-turn cap to avoid infinite loops
		if (gameState.aiActionsThisTurn >= 8) {
			gameState.domainGameManager.endTurn(game.model.GameState.P2);
			gameState.aiTurnActive = false;
			gameState.aiCooldownTicks = 1;

			game.ui.TemplateCommandDispatcher.renderAllUnits(out, gameState, gameState.domainState);
			game.ui.TemplateCommandDispatcher.renderPlayerStats(out, gameState.domainState);
			game.ui.TemplateCommandDispatcher.showNotification(out, "Your turn.");
			return;
		}

		// Execute exactly ONE AI step
		game.ai.AIController.StepResult r = gameState.aiController.step(gameState.domainState);
		if (r == null || r.type == null) return;

		// Classic switch (avoid newer switch-> syntax to be safe with older Java)
		switch (r.type) {

			case NONE:
				// No legal actions -> end turn
				gameState.domainGameManager.endTurn(game.model.GameState.P2);
				gameState.aiTurnActive = false;
				gameState.aiCooldownTicks = 1;

				game.ui.TemplateCommandDispatcher.renderAllUnits(out, gameState, gameState.domainState);
				game.ui.TemplateCommandDispatcher.renderPlayerStats(out, gameState.domainState);
				game.ui.TemplateCommandDispatcher.showNotification(out, "Your turn.");
				break;

			case MOVE:
				gameState.aiActionsThisTurn++;

				// Move animation: lock input here, unlock via UnitStopped event
				game.model.Unit moved = gameState.domainState.getBoard().getUnitById(r.unitId).orElse(null);
				if (moved != null) {
					game.ui.TemplateCommandDispatcher.moveUnit(out, gameState, moved);
					gameState.inputLocked = true; // must be unlocked by UnitStopped
				}
				break;

			case ATTACK:
			case PLAY_CARD:
				gameState.aiActionsThisTurn++;

				// No explicit UI "ack" for these; refresh state and add a small cooldown
				game.ui.TemplateCommandDispatcher.renderAllUnits(out, gameState, gameState.domainState);
				game.ui.TemplateCommandDispatcher.renderPlayerStats(out, gameState.domainState);
				gameState.aiCooldownTicks = 1;
				break;

			case END_TURN:
				gameState.aiTurnActive = false;
				gameState.aiCooldownTicks = 1;

				game.ui.TemplateCommandDispatcher.renderAllUnits(out, gameState, gameState.domainState);
				game.ui.TemplateCommandDispatcher.renderPlayerStats(out, gameState.domainState);
				game.ui.TemplateCommandDispatcher.showNotification(out, "Your turn.");
				break;

			default:
				break;
		}
	}

}