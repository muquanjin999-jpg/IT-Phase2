package game.system.action;

import game.model.Board;
import game.model.TilePos;
import game.model.Unit;

import java.util.*;

public class ReachabilityService {

    public List<TilePos> reachableTiles(Board board, Unit unit) {
        if (board == null || unit == null) return Collections.emptyList();
        if (unit.getMoveRemaining() <= 0) return Collections.emptyList();
        if (unit.getFrozenTurns() > 0) return Collections.emptyList();
        if (unit.getMoveRange() <= 0) return Collections.emptyList();

        TilePos start = unit.getPosition();
        if (start == null) return Collections.emptyList();

        Queue<TilePos> q = new ArrayDeque<>();
        Map<TilePos, Integer> dist = new HashMap<>();
        q.add(start);
        dist.put(start, 0);

        while (!q.isEmpty()) {
            TilePos cur = q.poll();
            int d = dist.get(cur);
            if (d >= unit.getMoveRange()) continue;

            for (TilePos nxt : board.getAdjacentOrthogonal(cur)) {
                if (dist.containsKey(nxt)) continue;
                boolean isStart = nxt.equals(start);
                if (!isStart && !board.isEmpty(nxt)) continue; // no passing through occupied tiles
                dist.put(nxt, d + 1);
                q.add(nxt);
            }
        }

        List<TilePos> result = new ArrayList<>();
        for (Map.Entry<TilePos, Integer> e : dist.entrySet()) {
            TilePos p = e.getKey();
            if (p.equals(start)) continue;
            if (e.getValue() <= unit.getMoveRange() && board.isEmpty(p)) {
                result.add(p);
            }
        }
        result.sort(Comparator.comparingInt(TilePos::x).thenComparingInt(TilePos::y));
        return result;
    }

    public boolean isReachable(Board board, Unit unit, TilePos destination) {
        return reachableTiles(board, unit).contains(destination);
    }
}
