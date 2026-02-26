package game.card.effect;

import game.card.CardPlayContext;
import game.model.TilePos;
import game.model.Unit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AssassinStrikeOnSummonEffect implements OnSummonEffect {
    private final int damage;

    public AssassinStrikeOnSummonEffect(int damage) {
        this.damage = Math.max(0, damage);
    }

    @Override
    public void apply(CardPlayContext ctx, String ownerId, Unit summonedUnit) {
        List<Unit> adjacentEnemies = new ArrayList<>();
        for (TilePos adj : ctx.getState().getBoard().getAdjacentOrthogonal(summonedUnit.getPosition())) {
            ctx.getState().getBoard().getUnitAt(adj).ifPresent(u -> {
                if (!u.getOwnerId().equals(ownerId)) adjacentEnemies.add(u);
            });
        }
        if (adjacentEnemies.isEmpty()) return;
        adjacentEnemies.sort(Comparator.comparingInt(Unit::getHp).thenComparing(Unit::getId));
        adjacentEnemies.get(0).damage(damage);
    }
}
