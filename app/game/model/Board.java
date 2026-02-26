package game.model;

import java.util.*;

public class Board {
    private final int width;
    private final int height;
    private final Map<TilePos, Unit> unitsByPos = new HashMap<>();
    private final Map<String, Unit> unitsById = new HashMap<>();

    public Board(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public boolean isInside(TilePos pos) {
        return pos != null && pos.x() >= 0 && pos.x() < width && pos.y() >= 0 && pos.y() < height;
    }

    public boolean isEmpty(TilePos pos) {
        return isInside(pos) && !unitsByPos.containsKey(pos);
    }

    public Optional<Unit> getUnitAt(TilePos pos) {
        return Optional.ofNullable(unitsByPos.get(pos));
    }

    public Optional<Unit> getUnitById(String unitId) {
        return Optional.ofNullable(unitsById.get(unitId));
    }

    public void placeUnit(Unit unit, TilePos pos) {
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(pos, "pos");
        if (!isInside(pos)) throw new IllegalArgumentException("Position out of board: " + pos);
        if (!isEmpty(pos)) throw new IllegalStateException("Tile occupied: " + pos);
        if (unitsById.containsKey(unit.getId())) throw new IllegalStateException("Unit already on board: " + unit.getId());
        unit.setPosition(pos);
        unitsByPos.put(pos, unit);
        unitsById.put(unit.getId(), unit);
    }

    public void moveUnit(String unitId, TilePos to) {
        Unit unit = unitsById.get(unitId);
        if (unit == null) throw new IllegalArgumentException("Unknown unit: " + unitId);
        if (!isInside(to)) throw new IllegalArgumentException("Destination out of board: " + to);
        if (!isEmpty(to)) throw new IllegalStateException("Destination occupied: " + to);

        TilePos from = unit.getPosition();
        unitsByPos.remove(from);
        unit.setPosition(to);
        unitsByPos.put(to, unit);
    }

    public Unit removeUnit(String unitId) {
        Unit unit = unitsById.remove(unitId);
        if (unit != null) {
            unitsByPos.remove(unit.getPosition());
        }
        return unit;
    }

    public List<Unit> getAllUnits() {
        return new ArrayList<>(unitsById.values());
    }

    public List<Unit> getUnitsByOwner(String ownerId) {
        List<Unit> result = new ArrayList<>();
        for (Unit u : unitsById.values()) {
            if (u.getOwnerId().equals(ownerId)) result.add(u);
        }
        return result;
    }

    public int manhattanDistance(TilePos a, TilePos b) {
        return a.manhattanDistance(b);
    }

    public List<TilePos> getAdjacentOrthogonal(TilePos pos) {
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        List<TilePos> out = new ArrayList<>(4);
        for (int[] d : dirs) {
            TilePos n = pos.add(d[0], d[1]);
            if (isInside(n)) out.add(n);
        }
        return out;
    }
}
