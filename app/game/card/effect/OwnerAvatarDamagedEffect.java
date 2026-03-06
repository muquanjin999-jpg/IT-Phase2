package game.card.effect;

import game.card.CardPlayContext;
import game.model.Unit;

public interface OwnerAvatarDamagedEffect {
    void onOwnerAvatarDamaged(CardPlayContext ctx, String ownerId, Unit unit, int damageDealt);
}