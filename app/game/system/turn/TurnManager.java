package game.system.turn;

import game.model.*;

import java.util.List;

public class TurnManager<T> {
    private final ManaSystem manaSystem;
    private final DrawSystem<T> drawSystem;

    public TurnManager(ManaSystem manaSystem, DrawSystem<T> drawSystem) {
        this.manaSystem = manaSystem;
        this.drawSystem = drawSystem;
    }

    public void initializeMatch(GameState<T> state) {
        // Draw starting hands (P1=3, P2=4 based on rules)
        drawSystem.drawInitialHand(state, GameState.P1, state.getRules().getInitialHandP1());
        drawSystem.drawInitialHand(state, GameState.P2, state.getRules().getInitialHandP2());

        // Put game into running state and start P1's first turn.
        state.setStatus(GameStatus.RUNNING);
        state.setActivePlayerId(GameState.P1);
        state.getPlayer(GameState.P1).setMaxMana(0);
        state.getPlayer(GameState.P2).setMaxMana(0);
        state.getPlayer(GameState.P1).setMana(0);
        state.getPlayer(GameState.P2).setMana(0);

        startTurnForActivePlayer(state);
    }

    public void startTurnForActivePlayer(GameState<T> state) {
        if (state.isGameOver()) return;

        Player<T> active = state.getActivePlayer();
        manaSystem.refreshAtStartTurn(active);

        // Reset counter flags for all units at the start of any player's turn (as specified)
        for (Unit unit : state.getBoard().getAllUnits()) {
            unit.setCounterUsed(false);
        }

        // Refresh only active player's units (including avatar if desired)
        List<Unit> ownUnits = state.getBoard().getUnitsByOwner(active.getId());
        for (Unit unit : ownUnits) {
            unit.refreshForOwnerTurn();
        }
        // Avatar refresh is explicit if avatar is not in board unit list; in our model avatar is placed on board,
        // but this is safe even if not.
        active.getAvatar().refreshForOwnerTurn();
    }

    public DrawResult<T> endTurnAndAdvance(GameState<T> state) {
        if (state.isGameOver()) {
            throw new IllegalStateException("Cannot end turn: game already over");
        }

        String endingPlayerId = state.getActivePlayerId();

        // End-turn draw belongs to the player who ends the turn (per your design)
        DrawResult<T> drawResult = drawSystem.drawOneForEndTurn(state, endingPlayerId);
        if (drawResult.getOutcomeType() == DrawOutcomeType.DECK_EMPTY_LOSS) {
            String loser = drawResult.getLoserPlayerId().orElseThrow(IllegalStateException::new);
            String winner = state.getOpponent(loser).getId();
            state.markWinner(winner);
            return drawResult;
        }

        // Switch active player and start their turn
        String nextPlayerId = state.getOpponent(endingPlayerId).getId();
        state.setActivePlayerId(nextPlayerId);
        state.incrementTurnNumber();
        startTurnForActivePlayer(state);
        return drawResult;
    }

    public ManaSystem getManaSystem() {
        return manaSystem;
    }

    public DrawSystem<T> getDrawSystem() {
        return drawSystem;
    }
}
