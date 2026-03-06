package game.card.effect;

import game.card.CardPlayContext;
import game.card.CardTarget;
import game.card.UnitTemplate;
import game.model.Board;
import game.model.TilePos;
import game.model.Unit;

public class DarkTerminusEffect implements Effect {

    private final UnitTemplate wraithlingTemplate;

    public DarkTerminusEffect(UnitTemplate wraithlingTemplate) {
        this.wraithlingTemplate = wraithlingTemplate;
    }

    @Override
    public void apply(CardPlayContext ctx, String casterPlayerId, CardTarget target) {
        if (target == null || target.getUnitId() == null) {
            throw new IllegalStateException("Dark Terminus requires unit target");
        }
        Unit enemy = ctx.getState().getBoard().getUnitById(target.getUnitId())
                .orElseThrow(() -> new IllegalStateException("Target unit not found: " + target.getUnitId()));
        if (enemy instanceof game.model.Avatar) {
            throw new IllegalStateException("Dark Terminus cannot target avatar");
        }
        TilePos tile = enemy.getPosition();

        enemy.damage(enemy.getHp() + 999);
        ctx.getDeathResolver().removeDeadUnitsAndCheckGameOver(ctx.getState());

        Board board = ctx.getState().getBoard();
        if (board.isInside(tile) && board.isEmpty(tile)) {
            Unit w = wraithlingTemplate.create(ctx.getIdGenerator().next("U"), casterPlayerId, tile);
            w.markSummonedThisTurn();
            board.placeUnit(w, tile);
        }
    }
}