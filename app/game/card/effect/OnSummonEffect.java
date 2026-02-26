package game.card.effect;

import game.card.CardPlayContext;
import game.model.Unit;

public interface OnSummonEffect {
    void apply(CardPlayContext ctx, String ownerId, Unit summonedUnit);
}
