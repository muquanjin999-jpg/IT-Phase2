package events;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import structures.GameState;

/**
 * Heartbeat event, fired roughly once per second by the UI loop.
 *
 * Drives the AI turn-loop:
 * P1 EndTurn -> aiTurnActive=true -> Heartbeat steps AI actions ->
 * (AnimationGate.lock(tag) ... AnimationEnded(tag) unlocks) -> AI EndTurn -> back to P1
 *
 * @author Dr. Richard McCreadie
 */
public class Heartbeat implements EventProcessor {

	@Override
	public void processEvent(ActorRef out, GameState gameState, JsonNode message) {

		if (gameState == null) return;
		if (!gameState.gameInitalised) return;
		if (gameState.domainGameManager == null || gameState.domainState == null) return;
		if (gameState.aiController == null) return;
		if (gameState.domainState.isGameOver()) return;

		// If any animation locks are active, wait for AnimationEnded(tag)
		if (gameState.animationGate != null && gameState.animationGate.isLocked()) {
			return;
		}

		// Fallback cooldown (only if UI never sends AnimationEnded)
		if (gameState.aiFallbackCooldownTicks > 0) {
			gameState.aiFallbackCooldownTicks--;
			return;
		}

		// Only run when AI loop is enabled AND it is P2's turn
		if (!gameState.aiTurnActive) return;
		if (!gameState.domainState.getActivePlayerId().equals(game.model.GameState.P2)) return;

		// Per-turn cap to avoid infinite loops
		if (gameState.aiActionsThisTurn >= 8) {
			gameState.domainGameManager.endTurn(game.model.GameState.P2);
			gameState.aiTurnActive = false;

			game.ui.TemplateCommandDispatcher.renderAllUnits(out, gameState, gameState.domainState);
			game.ui.TemplateCommandDispatcher.renderPlayerStats(out, gameState.domainState);
			game.ui.TemplateCommandDispatcher.renderHand(out, gameState, gameState.domainState, game.model.GameState.P1);
			game.ui.TemplateCommandDispatcher.showNotification(out, "Your turn.");
			return;
		}

		// Execute exactly ONE AI step
		game.ai.AIController.StepResult r = gameState.aiController.step(gameState.domainState);
		if (r == null || r.type == null) return;

		switch (r.type) {

			case NONE:
				// No legal actions -> end turn
				gameState.domainGameManager.endTurn(game.model.GameState.P2);
				gameState.aiTurnActive = false;

				game.ui.TemplateCommandDispatcher.renderAllUnits(out, gameState, gameState.domainState);
				game.ui.TemplateCommandDispatcher.renderPlayerStats(out, gameState.domainState);
				game.ui.TemplateCommandDispatcher.renderHand(out, gameState, gameState.domainState, game.model.GameState.P1);
				game.ui.TemplateCommandDispatcher.showNotification(out, "Your turn.");
				break;

			case MOVE:
				gameState.aiActionsThisTurn++;
				// Lock on a movement tag; UnitStopped can also unlock, but we support AnimationEnded too
				if (gameState.animationGate != null) gameState.animationGate.lock("AI_MOVE");

				game.model.Unit moved = gameState.domainState.getBoard().getUnitById(r.unitId).orElse(null);
				if (moved != null) {
					game.ui.TemplateCommandDispatcher.moveUnit(out, gameState, moved);
				}

				// If UI never sends AnimationEnded(tag), fallback unlock after 1 tick
				gameState.aiFallbackCooldownTicks = 1;
				break;

			case ATTACK:
			case PLAY_CARD:
				gameState.aiActionsThisTurn++;
				// Lock on action tag; AnimationEnded(tag) should unlock
				if (gameState.animationGate != null) gameState.animationGate.lock("AI_ACTION");

				// Refresh board/stats/hand
				game.ui.TemplateCommandDispatcher.renderAllUnits(out, gameState, gameState.domainState);
				game.ui.TemplateCommandDispatcher.renderPlayerStats(out, gameState.domainState);
				game.ui.TemplateCommandDispatcher.renderHand(out, gameState, gameState.domainState, game.model.GameState.P1);

				// fallback if no ack
				gameState.aiFallbackCooldownTicks = 1;
				break;

			case END_TURN:
				gameState.aiTurnActive = false;

				game.ui.TemplateCommandDispatcher.renderAllUnits(out, gameState, gameState.domainState);
				game.ui.TemplateCommandDispatcher.renderPlayerStats(out, gameState.domainState);
				game.ui.TemplateCommandDispatcher.renderHand(out, gameState, gameState.domainState, game.model.GameState.P1);
				game.ui.TemplateCommandDispatcher.showNotification(out, "Your turn.");
				break;

			default:
				break;
		}
	}

}