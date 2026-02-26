package game.card;

import game.card.effect.Effect;
import game.model.Player;
import game.system.action.ValidationResult;

public class SpellCard extends Card {
    private final TargetSpec targetSpec;
    private final Effect effect;

    public SpellCard(String id, String name, int cost, TargetSpec targetSpec, Effect effect) {
        super(id, name, cost, CardType.SPELL);
        this.targetSpec = targetSpec;
        this.effect = effect;
    }

    public TargetSpec getTargetSpec() { return targetSpec; }
    public Effect getEffect() { return effect; }

    @Override
    public void play(CardPlayContext ctx, String ownerId, CardTarget target) {
        ValidationResult vr = ctx.getActionValidator().validateSpellTarget(ctx.getState(), ownerId, this, target);
        if (!vr.isOk()) {
            throw new IllegalStateException(vr.getMessage());
        }

        Player<Card> player = ctx.getState().getPlayer(ownerId);
        ctx.getManaSystem().spend(player, getCost());
        effect.apply(ctx, ownerId, target);
        ctx.getDeathResolver().removeDeadUnitsAndCheckGameOver(ctx.getState());
    }
}
