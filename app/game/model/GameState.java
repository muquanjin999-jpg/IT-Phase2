package game.model;

import java.util.Objects;

public class GameState<T> {
    public static final String P1 = "P1";
    public static final String P2 = "P2";

    private final GameRules rules;
    private final Board board;
    private final Player<T> player1;
    private final Player<T> player2;

    private String activePlayerId;
    private int turnNumber;
    private GameStatus status;

    public GameState(GameRules rules, Board board, Player<T> player1, Player<T> player2) {
        this.rules = Objects.requireNonNull(rules);
        this.board = Objects.requireNonNull(board);
        this.player1 = Objects.requireNonNull(player1);
        this.player2 = Objects.requireNonNull(player2);
        this.activePlayerId = P1;
        this.turnNumber = 1;
        this.status = GameStatus.NOT_STARTED;
    }

    public GameRules getRules() { return rules; }
    public Board getBoard() { return board; }

    public Player<T> getPlayer1() { return player1; }
    public Player<T> getPlayer2() { return player2; }

    public Player<T> getActivePlayer() {
        return getPlayer(activePlayerId);
    }

    public Player<T> getNonActivePlayer() {
        return getOpponent(activePlayerId);
    }

    public Player<T> getPlayer(String playerId) {
        if (P1.equals(playerId)) return player1;
        if (P2.equals(playerId)) return player2;
        throw new IllegalArgumentException("Unknown playerId: " + playerId);
    }

    public Player<T> getOpponent(String playerId) {
        return getPlayer(P1.equals(playerId) ? P2 : P1);
    }

    public String getActivePlayerId() { return activePlayerId; }
    public void setActivePlayerId(String activePlayerId) { this.activePlayerId = activePlayerId; }

    public int getTurnNumber() { return turnNumber; }
    public void incrementTurnNumber() { this.turnNumber += 1; }

    public GameStatus getStatus() { return status; }
    public void setStatus(GameStatus status) { this.status = status; }

    public boolean isGameOver() {
        return status == GameStatus.P1_WIN || status == GameStatus.P2_WIN;
    }

    public void markWinner(String winnerId) {
        if (P1.equals(winnerId)) this.status = GameStatus.P1_WIN;
        else if (P2.equals(winnerId)) this.status = GameStatus.P2_WIN;
        else throw new IllegalArgumentException("Unknown winnerId: " + winnerId);
    }
}
