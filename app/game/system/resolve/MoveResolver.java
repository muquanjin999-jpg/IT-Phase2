package game.system.resolve;

import game.card.Card;
import game.model.GameState;
import game.model.TilePos;
import game.model.Unit;

public class MoveResolver {

    public void resolveMove(GameState<Card> state, String unitId, TilePos destination) {
        Unit unit = state.getBoard().getUnitById(unitId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown unit: " + unitId));

        state.getBoard().moveUnit(unitId, destination);
        unit.setMoveRemaining(0);
        unit.setHasMoved(true);
    }
}
