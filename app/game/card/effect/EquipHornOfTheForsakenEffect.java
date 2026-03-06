package game.card.effect;

import game.card.CardPlayContext;
import game.card.CardTarget;

public class EquipHornOfTheForsakenEffect implements Effect {

    private final int robustness;

    public EquipHornOfTheForsakenEffect(int robustness) {
        this.robustness = Math.max(0, robustness);
    }

    @Override
    public void apply(CardPlayContext ctx, String casterPlayerId, CardTarget target) {
        ctx.getState().getPlayer(casterPlayerId).equipHornOfTheForsaken(robustness);
    }
}