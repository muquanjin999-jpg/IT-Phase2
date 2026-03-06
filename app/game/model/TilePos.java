package game.model;

import java.util.Objects;

public final class TilePos {
    private final int x;
    private final int y;

    public TilePos(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int x() { return x; }
    public int y() { return y; }
    
    public int getX() { return x; }
    public int getY() { return y; }

    public int manhattanDistance(TilePos other) {
        return Math.abs(this.x - other.x) + Math.abs(this.y - other.y);
    }

    public TilePos add(int dx, int dy) {
        return new TilePos(this.x + dx, this.y + dy);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TilePos)) return false;
        TilePos tilePos = (TilePos) o;
        return x == tilePos.x && y == tilePos.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }
}
