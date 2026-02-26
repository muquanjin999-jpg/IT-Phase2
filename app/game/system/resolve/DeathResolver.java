package game.system.resolve;

import game.model.Avatar;
import game.model.GameState;
import game.model.Unit;

import java.util.ArrayList;
import java.util.List;

public class DeathResolver {

    public void removeDeadUnitsAndCheckGameOver(GameState<?> state) {
        List<Unit> dead = new ArrayList<>();
        for (Unit u : state.getBoard().getAllUnits()) {
            if (u.isDead()) dead.add(u);
        }

        for (Unit u : dead) {
            state.getBoard().removeUnit(u.getId());
            if (u instanceof Avatar) {
                String winnerId = state.getOpponent(u.getOwnerId()).getId();
                state.markWinner(winnerId);
            }
        }
    }
}
