package game.card.effect;

import game.card.CardPlayContext;
import game.card.CardTarget;
import game.card.UnitTemplate;
import game.model.Unit;

public class SummonUnitEffect implements Effect {
    private final UnitTemplate template;

    public SummonUnitEffect(UnitTemplate template) {
        this.template = template;
    }

    @Override
    public void apply(CardPlayContext ctx, String casterPlayerId, CardTarget target) {
        if (target == null || target.getTile() == null) {
            throw new IllegalStateException("SummonUnitEffect requires tile target");
        }
        if (!ctx.getState().getBoard().isEmpty(target.getTile())) {
            throw new IllegalStateException("Summon tile occupied");
        }
        Unit summoned = template.create(ctx.getIdGenerator().next("U"), casterPlayerId, target.getTile());
        summoned.markSummonedThisTurn();
        ctx.getState().getBoard().placeUnit(summoned, target.getTile());
    }
}
