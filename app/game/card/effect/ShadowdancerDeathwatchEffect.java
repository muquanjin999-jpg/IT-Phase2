package game.card.effect;

import game.card.CardPlayContext;
import game.model.Avatar;
import game.model.Unit;

public class ShadowdancerDeathwatchEffect implements DeathwatchEffect {

    @Override
    public void onUnitDied(CardPlayContext ctx, String ownerId, Unit watcher, Unit deadUnit) {
        if (watcher == null || watcher.isDead()) return;

        String enemyId = ownerId.equals(game.model.GameState.P1) ? game.model.GameState.P2 : game.model.GameState.P1;
        Avatar enemyAvatar = (Avatar) ctx.getState().getPlayer(enemyId).getAvatar();
        enemyAvatar.damage(1);
        watcher.heal(1);
    }
}