package game.card.effect;

import game.card.CardPlayContext;
import game.model.Unit;

public class BuffSelfAttackOnDeathwatchEffect implements DeathwatchEffect {
    private final int atk;

    public BuffSelfAttackOnDeathwatchEffect(int atk) {
        this.atk = Math.max(0, atk);
    }

    @Override
    public void onUnitDied(CardPlayContext ctx, String ownerId, Unit watcher, Unit deadUnit) {
        if (watcher == null || watcher.isDead()) return;
        watcher.setAttack(watcher.getAttack() + atk);
    }
}