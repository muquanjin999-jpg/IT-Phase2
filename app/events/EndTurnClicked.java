package events;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import structures.GameState;

/**
 * Indicates that the user has clicked an object on the game canvas, in this case
 * the end-turn button.
 * 
 * { 
 *   messageType = “endTurnClicked”
 * }
 * 
 * @author Dr. Richard McCreadie
 *
 */
public class EndTurnClicked implements EventProcessor{

	@Override
	public void processEvent(ActorRef out, GameState gameState, JsonNode message) {
		if (!gameState.gameInitalised || gameState.domainGameManager == null || gameState.domainState == null) return;
		if (gameState.inputLocked) return;
		if (!gameState.domainState.getActivePlayerId().equals(game.model.GameState.P1)) return;

		game.system.action.ValidationResult vr = gameState.domainGameManager.endTurn(game.model.GameState.P1);
		if (!vr.isOk()) {
			game.ui.TemplateCommandDispatcher.showNotification(out, vr.getMessage());
			return;
		}

		// Clear any selections/highlights
		gameState.selectedUnitId = null;
		gameState.selectedHandPos = null;
		game.ui.TemplateCommandDispatcher.clearTileHighlights(out, gameState);
		gameState.highlightedTargets.clear();

		// Update stats
		game.ui.TemplateCommandDispatcher.renderPlayerStats(out, gameState.domainState);
		game.ui.TemplateCommandDispatcher.showNotification(out, "Turn ended.");
		
		// If it's now AI's turn, start AI loop (executed step-by-step in Heartbeat)
		if (gameState.domainState.getActivePlayerId().equals(game.model.GameState.P2)) {
			gameState.aiTurnActive = true;
			gameState.aiActionsThisTurn = 0;
			gameState.aiCooldownTicks = 1; // give UI one heartbeat to settle
			game.ui.TemplateCommandDispatcher.showNotification(out, "AI turn...");
		}
	}

}
