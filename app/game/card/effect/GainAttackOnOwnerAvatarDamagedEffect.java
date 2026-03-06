package game.card.effect;

import game.card.CardPlayContext;
import game.model.Unit;

public class GainAttackOnOwnerAvatarDamagedEffect implements OwnerAvatarDamagedEffect {
    private final int atk;

    public GainAttackOnOwnerAvatarDamagedEffect(int atk) {
        this.atk = Math.max(0, atk);
    }

    @Override
    public void onOwnerAvatarDamaged(CardPlayContext ctx, String ownerId, Unit unit, int damageDealt) {
        if (unit == null || unit.isDead()) return;
        unit.setAttack(unit.getAttack() + atk);
    }
}