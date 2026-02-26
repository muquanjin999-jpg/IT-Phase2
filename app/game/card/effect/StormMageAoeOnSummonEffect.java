package game.card.effect;

import game.card.CardPlayContext;
import game.model.TilePos;
import game.model.Unit;

public class StormMageAoeOnSummonEffect implements OnSummonEffect {
    private final int damage;

    public StormMageAoeOnSummonEffect(int damage) {
        this.damage = Math.max(0, damage);
    }

    @Override
    public void apply(CardPlayContext ctx, String ownerId, Unit summonedUnit) {
        for (TilePos adj : ctx.getState().getBoard().getAdjacentOrthogonal(summonedUnit.getPosition())) {
            ctx.getState().getBoard().getUnitAt(adj).ifPresent(u -> {
                if (!u.getOwnerId().equals(ownerId)) {
                    u.damage(damage);
                }
            });
        }
    }
}
