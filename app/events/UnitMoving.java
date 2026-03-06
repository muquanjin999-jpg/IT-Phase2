package events;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import structures.GameState;

/**
 * Indicates that a unit instance has started moving.
 *
 * {
 *   messageType = "unitMoving"
 *   id = <unit id>
 * }
 */
public class UnitMoving implements EventProcessor {

    @Override
    public void processEvent(ActorRef out, GameState gameState, JsonNode message) {

        if (gameState == null) return;
        if (!gameState.gameInitalised || gameState.domainState == null) return;

        // IMPORTANT:
        // Do NOT set inputLocked here.
        // unitMoving can fire frequently; locking input here easily causes "sticky" Input locked.
        // Input locking should be done where the action is initiated (TileClicked/CardClicked/AI step).
        //
        // Optionally, lock MOVE gate only (safe), but even this is not strictly necessary
        // if you already lock MOVE when you issue the move command.

        if (gameState.animationGate != null) {
            // Only lock if not already locked, to avoid stacking weird states.
            if (!gameState.animationGate.isLocked()) {
                gameState.animationGate.lock(game.ui.AnimationTags.MOVE);
            }
        }

        // Reset timer so Heartbeat timeout works predictably (optional)
        gameState.animationLockTicks = 0;
    }
}