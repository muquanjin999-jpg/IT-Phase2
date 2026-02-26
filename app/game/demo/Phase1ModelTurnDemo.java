package game.demo;

import game.model.*;
import game.system.turn.*;

import java.util.ArrayList;
import java.util.List;

public class Phase1ModelTurnDemo {
    public static void main(String[] args) {
        GameRules rules = GameRules.defaultRules();

        // Phase 1 uses String card IDs as placeholder card objects.
        List<String> deckP1Cards = new ArrayList<>();
        List<String> deckP2Cards = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            deckP1Cards.add("P1_CARD_" + i);
            deckP2Cards.add("P2_CARD_" + i);
        }

        Player<String> p1 = buildPlayer(GameState.P1, "Player 1", rules, deckP1Cards, new TilePos(1, 2));
        Player<String> p2 = buildPlayer(GameState.P2, "Player 2", rules, deckP2Cards, new TilePos(7, 2));

        Board board = new Board(rules.getBoardWidth(), rules.getBoardHeight());
        board.placeUnit(p1.getAvatar(), p1.getAvatar().getPosition());
        board.placeUnit(p2.getAvatar(), p2.getAvatar().getPosition());

        // Put a couple of sample units on board to show start-turn refresh behavior
        Unit soldier1 = new Unit("u1", GameState.P1, "P1 Soldier", 3, 4, 2, 1, new TilePos(2,2));
        soldier1.addKeyword(UnitKeyword.ATTACK_TWICE);
        soldier1.setFrozenTurns(1); // should skip actions this turn, then decrement
        board.placeUnit(soldier1, soldier1.getPosition());

        Unit soldier2 = new Unit("u2", GameState.P2, "P2 Guard", 2, 5, 2, 1, new TilePos(6,2));
        soldier2.addKeyword(UnitKeyword.PROTECTOR);
        board.placeUnit(soldier2, soldier2.getPosition());

        GameState<String> state = new GameState<>(rules, board, p1, p2);

        TurnManager<String> turnManager = new TurnManager<>(
                new ManaSystem(rules.getManaCap()),
                new DrawSystem<>()
        );

        turnManager.initializeMatch(state);

        printState("After initializeMatch (P1 start turn)", state);

        DrawResult<String> end1 = turnManager.endTurnAndAdvance(state);
        System.out.println("\nP1 EndTurn draw result = " + end1);
        printState("After P1 end turn -> P2 start turn", state);

        DrawResult<String> end2 = turnManager.endTurnAndAdvance(state);
        System.out.println("\nP2 EndTurn draw result = " + end2);
        printState("After P2 end turn -> P1 start turn", state);
    }

    private static Player<String> buildPlayer(String id, String name, GameRules rules, List<String> deckCards, TilePos avatarPos) {
        Avatar avatar = new Avatar("avatar-" + id, id, name + " Avatar", rules.getAvatarHp(), avatarPos);
        Deck<String> deck = new Deck<>(deckCards);
        Hand<String> hand = new Hand<>(rules.getHandLimit());
        DiscardPile<String> discardPile = new DiscardPile<>();
        return new Player<>(id, name, avatar, deck, hand, discardPile);
    }

    private static void printState(String title, GameState<String> state) {
        System.out.println("\n=== " + title + " ===");
        System.out.println("Status: " + state.getStatus() + ", turnNumber=" + state.getTurnNumber() + ", active=" + state.getActivePlayerId());
        System.out.println("P1 => " + state.getPlayer1() + ", hand=" + state.getPlayer1().getHand());
        System.out.println("P2 => " + state.getPlayer2() + ", hand=" + state.getPlayer2().getHand());
        System.out.println("Board units:");
        for (Unit u : state.getBoard().getAllUnits()) {
            System.out.println("  " + u);
        }
    }
}
