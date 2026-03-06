package game.card.effect;

import game.card.CardPlayContext;
import game.model.Unit;

public interface DeathwatchEffect {
    void onUnitDied(CardPlayContext ctx, String ownerId, Unit watcher, Unit deadUnit);
}