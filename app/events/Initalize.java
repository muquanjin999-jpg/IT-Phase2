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
 *
 */
public class Initalize implements EventProcessor{

	@Override
	public void processEvent(ActorRef out, GameState gameState, JsonNode message) {
		// Mark the template GameState as initialised.
		gameState.gameInitalised = true;

		// Create / reset the domain game manager + domain game state.
		gameState.domainGameManager = new game.core.GameManager();
		gameState.domainState = gameState.domainGameManager.initializeNewGame();
		
		// Init AI controller + reset AI loop flags
		gameState.aiController = new game.ai.AIController(gameState.domainGameManager);
		gameState.aiTurnActive = false;
		gameState.aiActionsThisTurn = 0;
		gameState.aiCooldownTicks = 0;

		// Minimal initial rendering: draw the grid + both avatars.
		TemplateCommandDispatcher.renderInitialBoardAndAvatars(out, gameState.domainState);
		TemplateCommandDispatcher.renderAllUnits(out, gameState, gameState.domainState);
		TemplateCommandDispatcher.renderPlayerStats(out, gameState.domainState);
		TemplateCommandDispatcher.showNotification(out, "Game started. Your turn.");
	}

}


