package events;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import structures.GameState;

/**
 * Fired when a unit finishes moving on the board.
 *
 * {
 *   messageType = "unitStopped"
 *   id = <unit id>
 *   tilex = <x>
 *   tiley = <y>
 * }
 */
public class UnitStopped implements EventProcessor {

    @Override
    public void processEvent(ActorRef out, GameState gameState, JsonNode message) {

        if (gameState == null) return;
        if (!gameState.gameInitalised || gameState.domainState == null) return;

        // Always unlock MOVE (movement completed)
        if (gameState.animationGate != null) {
            gameState.animationGate.unlock(game.ui.AnimationTags.MOVE);
        }

        // HARD RELEASE:
        // unitStopped is the end of the movement animation, so input must be released.
        // Do NOT depend on isLocked() which may include other stale tags.
        gameState.inputLocked = false;

        // Reset timers / fallback counters to avoid sticky lock behaviour
        gameState.animationLockTicks = 0;
        gameState.aiFallbackCooldownTicks = 0;

        // Optional: clear selections & highlights (prevents odd UI states after move)
        gameState.selectedUnitId = null;
        game.ui.TemplateCommandDispatcher.clearTileHighlights(out, gameState);
        gameState.highlightedTargets.clear();

        // Sync UI
        game.ui.TemplateCommandDispatcher.renderAllUnits(out, gameState, gameState.domainState);
        game.ui.TemplateCommandDispatcher.renderPlayerStats(out, gameState.domainState);
        game.ui.TemplateCommandDispatcher.renderHand(out, gameState, gameState.domainState, game.model.GameState.P1);
    }
}