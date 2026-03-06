package game.card.effect;

import game.card.CardPlayContext;
import game.card.CardTarget;
import game.card.UnitTemplate;
import game.model.Board;
import game.model.TilePos;
import game.model.Unit;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SummonThreeWraithlingsEffect implements Effect {
    private final UnitTemplate wraithlingTemplate;
    private final Random rng = new Random();

    public SummonThreeWraithlingsEffect(UnitTemplate wraithlingTemplate) {
        this.wraithlingTemplate = wraithlingTemplate;
    }

    @Override
    public void apply(CardPlayContext ctx, String casterPlayerId, CardTarget target) {
        if (target == null || target.getTile() == null) {
            throw new IllegalStateException("Wraithling Swarm requires tile target");
        }
        Board board = ctx.getState().getBoard();
        TilePos base = target.getTile();
        if (!board.isInside(base) || !board.isEmpty(base)) {
            throw new IllegalStateException("Target tile must be empty");
        }

        summonAt(ctx, casterPlayerId, base);

        for (int i = 0; i < 2; i++) {
            List<TilePos> empties = new ArrayList<>();
            for (TilePos adj : board.getAdjacentOrthogonal(base)) {
                if (board.isInside(adj) && board.isEmpty(adj)) empties.add(adj);
            }
            if (empties.isEmpty()) break;
            TilePos chosen = empties.get(rng.nextInt(empties.size()));
            summonAt(ctx, casterPlayerId, chosen);
        }
    }

    private void summonAt(CardPlayContext ctx, String ownerId, TilePos p) {
        Unit w = wraithlingTemplate.create(ctx.getIdGenerator().next("U"), ownerId, p);
        w.markSummonedThisTurn();
        ctx.getState().getBoard().placeUnit(w, p);
    }
}