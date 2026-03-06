package game.card.effect;

import game.card.CardPlayContext;
import game.card.UnitTemplate;
import game.model.Board;
import game.model.TilePos;
import game.model.Unit;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SummonWraithlingAdjacentOnDeathwatchEffect implements DeathwatchEffect {
    private final UnitTemplate wraithlingTemplate;
    private final Random rng = new Random();

    public SummonWraithlingAdjacentOnDeathwatchEffect(UnitTemplate wraithlingTemplate) {
        this.wraithlingTemplate = wraithlingTemplate;
    }

    @Override
    public void onUnitDied(CardPlayContext ctx, String ownerId, Unit watcher, Unit deadUnit) {
        if (watcher == null || watcher.isDead()) return;
        Board board = ctx.getState().getBoard();

        List<TilePos> empties = new ArrayList<>();
        for (TilePos adj : board.getAdjacentOrthogonal(watcher.getPosition())) {
            if (board.isInside(adj) && board.isEmpty(adj)) empties.add(adj);
        }
        if (empties.isEmpty()) return;

        TilePos chosen = empties.get(rng.nextInt(empties.size()));
        Unit summoned = wraithlingTemplate.create(ctx.getIdGenerator().next("U"), ownerId, chosen);
        summoned.markSummonedThisTurn();
        board.placeUnit(summoned, chosen);
    }
}