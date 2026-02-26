package game.card.effect;

import game.card.CardPlayContext;
import game.card.CardTarget;

public interface Effect {
    void apply(CardPlayContext ctx, String casterPlayerId, CardTarget target);
}
