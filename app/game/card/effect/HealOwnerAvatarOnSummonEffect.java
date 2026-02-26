package game.card.effect;

import game.card.CardPlayContext;
import game.model.Unit;

public class HealOwnerAvatarOnSummonEffect implements OnSummonEffect {
    private final int amount;

    public HealOwnerAvatarOnSummonEffect(int amount) {
        this.amount = Math.max(0, amount);
    }

    @Override
    public void apply(CardPlayContext ctx, String ownerId, Unit summonedUnit) {
        ctx.getState().getPlayer(ownerId).getAvatar().heal(amount);
    }
}
