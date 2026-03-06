package events;

import com.fasterxml.jackson.databind.JsonNode;
import akka.actor.ActorRef;
import structures.GameState;

public class AnimationEnded implements EventProcessor {

    @Override
    public void processEvent(ActorRef out, GameState gameState, JsonNode message) {

        if (gameState == null) return;
        if (!gameState.gameInitalised || gameState.domainState == null) return;

        // --- HARD UNLOCK ---
        // We do not trust tag matching; any animationEnded releases the lock.
        if (gameState.animationGate != null) {
            gameState.animationGate.unlockAll();
        }

        gameState.inputLocked = false;
        gameState.animationLockTicks = 0;
        gameState.aiFallbackCooldownTicks = 0;

        // Reset selections
        game.ui.TemplateCommandDispatcher.clearTileHighlights(out, gameState);
        gameState.highlightedTargets.clear();
        gameState.selectedUnitId = null;
        gameState.selectedHandPos = null;

        // Sync UI with domain state
        game.ui.TemplateCommandDispatcher.renderAllUnits(out, gameState, gameState.domainState);
        game.ui.TemplateCommandDispatcher.renderPlayerStats(out, gameState.domainState);
        game.ui.TemplateCommandDispatcher.renderHand(out, gameState, gameState.domainState, game.model.GameState.P1);
    }
}