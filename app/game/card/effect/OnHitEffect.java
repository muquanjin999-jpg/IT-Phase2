package game.card.effect;

import game.card.CardPlayContext;
import game.model.Unit;

public interface OnHitEffect {
    void onHitEnemyUnit(CardPlayContext ctx, String ownerId, Unit source, Unit enemyTarget, int damageDealt);
}