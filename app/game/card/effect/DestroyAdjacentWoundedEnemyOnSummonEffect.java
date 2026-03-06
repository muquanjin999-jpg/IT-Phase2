package game.card.effect;

import game.card.CardPlayContext;
import game.model.Board;
import game.model.TilePos;
import game.model.Unit;

import java.util.Optional;

public class DestroyAdjacentWoundedEnemyOnSummonEffect implements OnSummonEffect {

    @Override
    public void apply(CardPlayContext ctx, String ownerId, Unit summonedUnit) {
        if (summonedUnit == null) return;
        Board board = ctx.getState().getBoard();

        for (TilePos adj : board.getAdjacentOrthogonal(summonedUnit.getPosition())) {
            Optional<Unit> opt = board.getUnitAt(adj);
            if (opt.isEmpty()) continue;
            Unit enemy = opt.get();
            if (enemy.getOwnerId().equals(ownerId)) continue;
            if (enemy instanceof game.model.Avatar) continue;
            if (enemy.getHp() < enemy.getMaxHp()) {
                enemy.damage(enemy.getHp() + 999);
                return;
            }
        }
    }
}