package game.model;

public final class GameRules {
    private final int boardWidth;
    private final int boardHeight;
    private final int avatarHp;
    private final int handLimit;
    private final int manaCap;
    private final int initialHandP1;
    private final int initialHandP2;

    public GameRules(int boardWidth, int boardHeight, int avatarHp, int handLimit, int manaCap, int initialHandP1, int initialHandP2) {
        this.boardWidth = boardWidth;
        this.boardHeight = boardHeight;
        this.avatarHp = avatarHp;
        this.handLimit = handLimit;
        this.manaCap = manaCap;
        this.initialHandP1 = initialHandP1;
        this.initialHandP2 = initialHandP2;
    }

    public static GameRules defaultRules() {
        return new GameRules(9, 5, 25, 6, 9, 3, 4);
    }

    public int getBoardWidth() { return boardWidth; }
    public int getBoardHeight() { return boardHeight; }
    public int getAvatarHp() { return avatarHp; }
    public int getHandLimit() { return handLimit; }
    public int getManaCap() { return manaCap; }
    public int getInitialHandP1() { return initialHandP1; }
    public int getInitialHandP2() { return initialHandP2; }
}
