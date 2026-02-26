package game.card;

import game.card.effect.OnSummonEffect;
import game.model.Player;
import game.model.Unit;
import game.system.action.ValidationResult;

public class UnitCard extends Card {
    private final UnitTemplate template;

    public UnitCard(String id, String name, int cost, UnitTemplate template) {
        super(id, name, cost, CardType.UNIT);
        this.template = template;
    }

    public UnitTemplate getTemplate() {
        return template;
    }

    @Override
    public void play(CardPlayContext ctx, String ownerId, CardTarget target) {
        ValidationResult vr = ctx.getActionValidator().validateSummon(ctx.getState(), ownerId, this, target == null ? null : target.getTile());
        if (!vr.isOk()) {
            throw new IllegalStateException(vr.getMessage());
        }

        Player<Card> player = ctx.getState().getPlayer(ownerId);
        ctx.getManaSystem().spend(player, getCost());

        Unit summoned = template.create(ctx.getIdGenerator().next("U"), ownerId, target.getTile());
        summoned.markSummonedThisTurn();
        ctx.getState().getBoard().placeUnit(summoned, target.getTile());

        for (OnSummonEffect effect : template.getOnSummonEffects()) {
            effect.apply(ctx, ownerId, summoned);
        }

        ctx.getDeathResolver().removeDeadUnitsAndCheckGameOver(ctx.getState());
    }
}
