package game.system.turn;

import java.util.Optional;

public class DrawResult<T> {
    private final DrawOutcomeType outcomeType;
    private final T card;
    private final String loserPlayerId; // only for DECK_EMPTY_LOSS

    private DrawResult(DrawOutcomeType outcomeType, T card, String loserPlayerId) {
        this.outcomeType = outcomeType;
        this.card = card;
        this.loserPlayerId = loserPlayerId;
    }

    public static <T> DrawResult<T> drawnToHand(T card) {
        return new DrawResult<>(DrawOutcomeType.DRAWN_TO_HAND, card, null);
    }

    public static <T> DrawResult<T> drawnToDiscard(T card) {
        return new DrawResult<>(DrawOutcomeType.DRAWN_TO_DISCARD, card, null);
    }

    public static <T> DrawResult<T> deckEmptyLoss(String loserPlayerId) {
        return new DrawResult<>(DrawOutcomeType.DECK_EMPTY_LOSS, null, loserPlayerId);
    }

    public DrawOutcomeType getOutcomeType() { return outcomeType; }
    public Optional<T> getCard() { return Optional.ofNullable(card); }
    public Optional<String> getLoserPlayerId() { return Optional.ofNullable(loserPlayerId); }

    @Override
    public String toString() {
        return "DrawResult{" +
                "outcomeType=" + outcomeType +
                ", card=" + card +
                ", loserPlayerId=" + loserPlayerId +
                '}';
    }
}
