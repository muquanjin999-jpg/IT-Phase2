package game.card.effect;

import game.card.CardPlayContext;
import game.model.Board;
import game.model.TilePos;
import game.model.Unit;

import java.util.Optional;

public class BuffAdjacentUnitNearAvatarOnSummonEffect implements OnSummonEffect {

    @Override
    public void apply(CardPlayContext ctx, String ownerId, Unit summonedUnit) {
        Board board = ctx.getState().getBoard();
        Unit avatar = ctx.getState().getPlayer(ownerId).getAvatar();
        TilePos av = avatar.getPosition();

        TilePos left = new TilePos(av.getX() - 1, av.getY());
        TilePos right = new TilePos(av.getX() + 1, av.getY());

        buffIfAlliedUnit(board, ownerId, left);
        buffIfAlliedUnit(board, ownerId, right);
    }

    private void buffIfAlliedUnit(Board board, String ownerId, TilePos p) {
        if (!board.isInside(p)) return;
        Optional<Unit> opt = board.getUnitAt(p);
        if (opt.isEmpty()) return;
        Unit u = opt.get();
        if (!u.getOwnerId().equals(ownerId)) return;
        if (u instanceof game.model.Avatar) return;

        u.setAttack(u.getAttack() + 1);
        u.increaseMaxHpAndHeal(1);
    }
}