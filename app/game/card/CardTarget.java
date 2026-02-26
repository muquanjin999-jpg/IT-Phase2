package game.card;

import game.model.TilePos;

public final class CardTarget {
    private final String unitId;
    private final TilePos tile;

    private CardTarget(String unitId, TilePos tile) {
        this.unitId = unitId;
        this.tile = tile;
    }

    public static CardTarget unit(String unitId) {
        return new CardTarget(unitId, null);
    }

    public static CardTarget tile(TilePos tile) {
        return new CardTarget(null, tile);
    }

    public static CardTarget unitToTile(String unitId, TilePos tile) {
        return new CardTarget(unitId, tile);
    }

    public String getUnitId() {
        return unitId;
    }

    public TilePos getTile() {
        return tile;
    }

    @Override
    public String toString() {
        return "CardTarget{unitId=" + unitId + ", tile=" + tile + "}";
    }
}
