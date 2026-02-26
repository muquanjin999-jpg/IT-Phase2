package game.system.turn;

import game.model.*;

import java.util.Optional;

public class DrawSystem<T> {

    public DrawResult<T> drawOneForEndTurn(GameState<T> state, String playerId) {
        Player<T> player = state.getPlayer(playerId);
        Optional<T> maybeCard = player.getDeck().draw();

        if (!maybeCard.isPresent()) {
            return DrawResult.deckEmptyLoss(playerId);
        }

        T card = maybeCard.get();
        if (player.getHand().isFull()) {
            player.getDiscardPile().add(card);
            return DrawResult.drawnToDiscard(card);
        } else {
            player.getHand().add(card);
            return DrawResult.drawnToHand(card);
        }
    }

    public void drawInitialHand(GameState<T> state, String playerId, int count) {
        Player<T> player = state.getPlayer(playerId);
        for (int i = 0; i < count; i++) {
            Optional<T> maybeCard = player.getDeck().draw();
            if (!maybeCard.isPresent()) {
                throw new IllegalStateException("Deck ran out while drawing initial hand for " + playerId);
            }
            if (!player.getHand().add(maybeCard.get())) {
                // Should not happen with default initial sizes, but keep deterministic behavior:
                player.getDiscardPile().add(maybeCard.get());
            }
        }
    }
}
