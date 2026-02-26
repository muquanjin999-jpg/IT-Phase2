package game.card.effect;

import game.card.CardPlayContext;
import game.card.CardTarget;
import game.model.Unit;

public class HealEffect implements Effect {
    private final int amount;

    public HealEffect(int amount) {
        this.amount = Math.max(0, amount);
    }

    @Override
    public void apply(CardPlayContext ctx, String casterPlayerId, CardTarget target) {
        if (target == null || target.getUnitId() == null) {
            throw new IllegalStateException("HealEffect requires unit target");
        }
        Unit unit = ctx.getState().getBoard().getUnitById(target.getUnitId())
                .orElseThrow(() -> new IllegalStateException("Target unit not found: " + target.getUnitId()));
        if (!unit.isDead()) {
            unit.heal(amount);
        }
    }
}
