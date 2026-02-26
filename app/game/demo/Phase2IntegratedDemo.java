package game.demo;

import game.card.Card;
import game.card.CardTarget;
import game.core.GameManager;
import game.model.GameState;
import game.model.TilePos;
import game.model.Unit;
import game.system.action.ValidationResult;

import java.util.Comparator;
import java.util.List;

public class Phase2IntegratedDemo {
    public static void main(String[] args) {
        GameManager gm = new GameManager();
        GameState<Card> state = gm.initializeNewGame();

        System.out.println("=== After init ===");
        System.out.println(gm.boardSummary());

        // Try to play the first playable card in P1 hand (if possible)
        int playableIndex = findFirstPlayableCardIndex(state);
        if (playableIndex >= 0) {
            Card card = state.getPlayer(GameState.P1).getHand().get(playableIndex);
            ValidationResult vr = tryAutoPlay(gm, state, playableIndex, card);
            System.out.println("Play card result: " + vr + " -> " + card.getName());
        }

        // Try a simple summon then move/attack demo if units exist
        Unit p1Unit = firstNonAvatarUnit(state, GameState.P1);
        if (p1Unit != null) {
            List<TilePos> reachable = gm.getReachableTiles(p1Unit.getId());
            System.out.println("Reachable for " + p1Unit.getId() + ": " + reachable);
            if (!reachable.isEmpty()) {
                ValidationResult mv = gm.moveUnit(GameState.P1, p1Unit.getId(), reachable.get(0));
                System.out.println("Move result: " + mv);
            }
        }

        System.out.println("\n=== State before end turn ===");
        System.out.println(gm.boardSummary());

        ValidationResult end = gm.endTurn(GameState.P1);
        System.out.println("End turn result: " + end);

        System.out.println("\n=== After P1 end turn ===");
        System.out.println(gm.boardSummary());
    }

    private static int findFirstPlayableCardIndex(GameState<Card> state) {
        for (int i = 0; i < state.getPlayer(GameState.P1).getHand().size(); i++) {
            Card c = state.getPlayer(GameState.P1).getHand().get(i);
            if (c.getCost() <= state.getPlayer(GameState.P1).getMana()) return i;
        }
        return -1;
    }

    private static ValidationResult tryAutoPlay(GameManager gm, GameState<Card> state, int handIndex, Card card) {
        // Naive auto-targeting for demo only.
        if (card.getType().name().equals("UNIT")) {
            // Find any legal tile adjacent to P1 avatar/unit
            for (int x = 0; x < state.getBoard().getWidth(); x++) {
                for (int y = 0; y < state.getBoard().getHeight(); y++) {
                    TilePos t = new TilePos(x, y);
                    ValidationResult vr = gm.playCardFromHand(GameState.P1, handIndex, CardTarget.tile(t));
                    if (vr.isOk()) return vr;
                }
            }
            return ValidationResult.fail("No legal summon tile found");
        } else {
            // Try target unit first
            List<Unit> units = state.getBoard().getAllUnits();
            units.sort(Comparator.comparing(Unit::getId));
            for (Unit u : units) {
                ValidationResult vr = gm.playCardFromHand(GameState.P1, handIndex, CardTarget.unit(u.getId()));
                if (vr.isOk()) return vr;
            }
            // Try tile target
            for (int x = 0; x < state.getBoard().getWidth(); x++) {
                for (int y = 0; y < state.getBoard().getHeight(); y++) {
                    TilePos t = new TilePos(x, y);
                    // Portal step requires unit+tile; this demo skips it unless a friendly unit exists
                    Unit src = firstNonAvatarUnit(state, GameState.P1);
                    ValidationResult vr;
                    if ("Portal Step".equals(card.getName()) && src != null) {
                        vr = gm.playCardFromHand(GameState.P1, handIndex, CardTarget.unitToTile(src.getId(), t));
                    } else {
                        vr = gm.playCardFromHand(GameState.P1, handIndex, CardTarget.tile(t));
                    }
                    if (vr.isOk()) return vr;
                }
            }
            return ValidationResult.fail("No legal spell target found");
        }
    }

    private static Unit firstNonAvatarUnit(GameState<Card> state, String ownerId) {
        return state.getBoard().getUnitsByOwner(ownerId).stream()
                .filter(u -> !(u instanceof game.model.Avatar))
                .findFirst().orElse(null);
    }
}
