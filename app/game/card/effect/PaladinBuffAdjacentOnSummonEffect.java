package game.card.effect;

import game.card.CardPlayContext;
import game.model.TilePos;
import game.model.Unit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PaladinBuffAdjacentOnSummonEffect implements OnSummonEffect {
    private final int attackBuff;
    private final int hpBuff;

    public PaladinBuffAdjacentOnSummonEffect(int attackBuff, int hpBuff) {
        this.attackBuff = attackBuff;
        this.hpBuff = hpBuff;
    }

    @Override
    public void apply(CardPlayContext ctx, String ownerId, Unit summonedUnit) {
        List<Unit> candidates = new ArrayList<>();
        for (TilePos adj : ctx.getState().getBoard().getAdjacentOrthogonal(summonedUnit.getPosition())) {
            ctx.getState().getBoard().getUnitAt(adj).ifPresent(u -> {
                if (u.getOwnerId().equals(ownerId) && !u.getId().equals(summonedUnit.getId())) {
                    candidates.add(u);
                }
            });
        }
        if (candidates.isEmpty()) return;
        candidates.sort(Comparator.comparing(Unit::getId));
        Unit target = candidates.get(0);
        target.setAttack(target.getAttack() + attackBuff);
        target.setMaxHp(target.getMaxHp() + hpBuff);
        target.heal(hpBuff);
    }
}
