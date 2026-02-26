package game.card.effect;

import game.card.CardPlayContext;
import game.card.CardTarget;
import game.model.Unit;

public class TeleportEffect implements Effect {
    @Override
    public void apply(CardPlayContext ctx, String casterPlayerId, CardTarget target) {
        if (target == null || target.getUnitId() == null || target.getTile() == null) {
            throw new IllegalStateException("TeleportEffect requires source unit + destination tile");
        }

        Unit source = ctx.getState().getBoard().getUnitById(target.getUnitId())
                .orElseThrow(() -> new IllegalStateException("Source unit not found: " + target.getUnitId()));

        if (!source.getOwnerId().equals(casterPlayerId)) {
            throw new IllegalStateException("Portal Step only teleports friendly units");
        }
        if (!ctx.getState().getBoard().isEmpty(target.getTile())) {
            throw new IllegalStateException("Destination tile is occupied");
        }

        // Does not consume move or reset actions.
        ctx.getState().getBoard().moveUnit(source.getId(), target.getTile());
    }
}
