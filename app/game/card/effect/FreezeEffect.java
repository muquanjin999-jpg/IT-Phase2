package game.card.effect;

import game.card.CardPlayContext;
import game.card.CardTarget;
import game.model.Unit;

public class FreezeEffect implements Effect {
    private final int turns;

    public FreezeEffect(int turns) {
        this.turns = Math.max(0, turns);
    }

    @Override
    public void apply(CardPlayContext ctx, String casterPlayerId, CardTarget target) {
        if (target == null || target.getUnitId() == null) {
            throw new IllegalStateException("FreezeEffect requires unit target");
        }
        Unit unit = ctx.getState().getBoard().getUnitById(target.getUnitId())
                .orElseThrow(() -> new IllegalStateException("Target unit not found: " + target.getUnitId()));
        unit.addFrozenTurns(turns);
    }
}
