package game.card.effect;

import game.card.CardPlayContext;
import game.card.CardTarget;
import game.model.Unit;

public class DestroyUnitEffect implements Effect {
    @Override
    public void apply(CardPlayContext ctx, String casterPlayerId, CardTarget target) {
        if (target == null || target.getUnitId() == null) {
            throw new IllegalStateException("DestroyUnitEffect requires unit target");
        }
        Unit unit = ctx.getState().getBoard().getUnitById(target.getUnitId())
                .orElseThrow(() -> new IllegalStateException("Target unit not found: " + target.getUnitId()));
        unit.damage(Math.max(0, unit.getHp()));
    }
}
