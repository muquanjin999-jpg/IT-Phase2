package events;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import structures.GameState;
import game.ui.TemplateCommandDispatcher;

/**
 * Indicates that both the core game loop in the browser is starting, meaning
 * that it is ready to recieve commands from the back-end.
 *
 * {
 *   messageType = “initalize”
 * }
 *
 * @author Dr. Richard McCreadie
 */
public class Initalize implements EventProcessor {

	@Override
	public void processEvent(ActorRef out, GameState gameState, JsonNode message) {
		// Mark the template GameState as initialised.
		gameState.gameInitalised = true;

		// Reset UI interaction state
		gameState.inputLocked = false;
		gameState.selectedUnitId = null;
		gameState.selectedHandPos = null;
		gameState.highlightedTiles.clear();
		gameState.highlightedTargets.clear();
		gameState.visualUnits.clear();
		gameState.visualHand.clear();
		gameState.animationGate = new game.ui.AnimationGate();

		// Create / reset the domain game manager + domain game state.
		gameState.domainGameManager = new game.core.GameManager();
		gameState.domainState = gameState.domainGameManager.initializeNewGame();

		// Init AI
		gameState.aiController = new game.ai.AIController(gameState.domainGameManager);
		gameState.aiTurnActive = false;
		gameState.aiActionsThisTurn = 0;
		gameState.aiFallbackCooldownTicks = 0;

		// Initial rendering: grid + avatars + units + stats + hand
		TemplateCommandDispatcher.renderInitialBoardAndAvatars(out, gameState.domainState);
		TemplateCommandDispatcher.renderAllUnits(out, gameState, gameState.domainState);
		TemplateCommandDispatcher.renderPlayerStats(out, gameState.domainState);

		// Step2: render P1 hand (positions 1..6)
		TemplateCommandDispatcher.renderHand(out, gameState, gameState.domainState, game.model.GameState.P1);

		TemplateCommandDispatcher.showNotification(out, "Game started. Your turn.");
	}

}