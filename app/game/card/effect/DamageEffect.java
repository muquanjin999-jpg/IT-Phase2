package game.card.effect;

import game.card.CardPlayContext;
import game.card.CardTarget;
import game.model.Unit;

public class DamageEffect implements Effect {
    private final int amount;

    public DamageEffect(int amount) {
        this.amount = Math.max(0, amount);
    }

    @Override
    public void apply(CardPlayContext ctx, String casterPlayerId, CardTarget target) {
        if (target == null || target.getUnitId() == null) {
            throw new IllegalStateException("DamageEffect requires unit target");
        }
        Unit unit = ctx.getState().getBoard().getUnitById(target.getUnitId())
                .orElseThrow(() -> new IllegalStateException("Target unit not found: " + target.getUnitId()));
        unit.damage(amount);
    }
}
