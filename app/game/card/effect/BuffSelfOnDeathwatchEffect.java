package game.card.effect;

import game.card.CardPlayContext;
import game.model.Unit;

public class BuffSelfOnDeathwatchEffect implements DeathwatchEffect {
    private final int atk;
    private final int hp;

    public BuffSelfOnDeathwatchEffect(int atk, int hp) {
        this.atk = Math.max(0, atk);
        this.hp = Math.max(0, hp);
    }

    @Override
    public void onUnitDied(CardPlayContext ctx, String ownerId, Unit watcher, Unit deadUnit) {
        if (watcher == null || watcher.isDead()) return;
        watcher.setAttack(watcher.getAttack() + atk);
        watcher.increaseMaxHpAndHeal(hp);
    }
}