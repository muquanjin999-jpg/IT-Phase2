package game.system.action;

import game.card.*;
import game.model.*;

public class ActionValidator {
    private final ReachabilityService reachabilityService;

    public ActionValidator(ReachabilityService reachabilityService) {
        this.reachabilityService = reachabilityService;
    }

    public java.util.List<TilePos> getReachableTiles(Board board, Unit unit) {
        if (board == null || unit == null) return java.util.List.of();
        return reachabilityService.reachableTiles(board, unit);
    }
    
    public ValidationResult validateMove(GameState<Card> state, String actorPlayerId, String unitId, TilePos destination) {
        if (state == null) return ValidationResult.fail("state is null");
        if (state.isGameOver()) return ValidationResult.fail("game is over");
        if (!state.getActivePlayerId().equals(actorPlayerId)) return ValidationResult.fail("not active player");

        Unit unit = state.getBoard().getUnitById(unitId).orElse(null);
        if (unit == null) return ValidationResult.fail("unit not found");
        if (!unit.getOwnerId().equals(actorPlayerId)) return ValidationResult.fail("cannot move enemy unit");
        if (destination == null) return ValidationResult.fail("destination is null");
        if (!state.getBoard().isInside(destination)) return ValidationResult.fail("destination outside board");
        if (!state.getBoard().isEmpty(destination)) return ValidationResult.fail("destination occupied");
        if (unit.getMoveRemaining() <= 0) return ValidationResult.fail("unit has no move action remaining");
        if (unit.getFrozenTurns() > 0) return ValidationResult.fail("unit is frozen");

        if (isProvoked(state, actorPlayerId, unit.getPosition())) {
            return ValidationResult.fail("unit is provoked and cannot move");
        }

        if (!reachabilityService.isReachable(state.getBoard(), unit, destination)) {
            return ValidationResult.fail("destination not reachable under path rules");
        }

        return ValidationResult.ok();
    }

    public ValidationResult validateAttack(GameState<Card> state, String actorPlayerId, String attackerId, String defenderId) {
        if (state == null) return ValidationResult.fail("state is null");
        if (state.isGameOver()) return ValidationResult.fail("game is over");
        if (!state.getActivePlayerId().equals(actorPlayerId)) return ValidationResult.fail("not active player");

        Unit attacker = state.getBoard().getUnitById(attackerId).orElse(null);
        if (attacker == null) return ValidationResult.fail("attacker not found");
        if (!attacker.getOwnerId().equals(actorPlayerId)) return ValidationResult.fail("attacker is not yours");
        if (attacker.getAttackRemaining() <= 0) return ValidationResult.fail("attacker has no attack action remaining");
        if (attacker.getFrozenTurns() > 0) return ValidationResult.fail("attacker is frozen");

        Unit defender = state.getBoard().getUnitById(defenderId).orElse(null);
        if (defender == null) return ValidationResult.fail("defender not found");
        if (defender.getOwnerId().equals(actorPlayerId)) return ValidationResult.fail("cannot attack friendly unit");
        if (defender.isDead()) return ValidationResult.fail("defender already dead");

        int dist = state.getBoard().manhattanDistance(attacker.getPosition(), defender.getPosition());
        if (dist > attacker.getAttackRange()) {
            return ValidationResult.fail("defender out of attack range");
        }

        if (isProvoked(state, actorPlayerId, attacker.getPosition()) && !defender.hasKeyword(UnitKeyword.PROVOKE)) {
            return ValidationResult.fail("provoked: must attack a Provoke unit");
        }

        if (defender instanceof Avatar && enemyHasProtector(state, actorPlayerId)) {
            return ValidationResult.fail("enemy avatar is protected by Protector unit");
        }

        return ValidationResult.ok();
    }

    public ValidationResult validateSummon(GameState<Card> state, String ownerId, UnitCard card, TilePos tile) {
        if (state == null) return ValidationResult.fail("state is null");
        if (tile == null) return ValidationResult.fail("summon tile is null");
        Board board = state.getBoard();

        if (!board.isInside(tile)) return ValidationResult.fail("summon tile outside board");
        if (!board.isEmpty(tile)) return ValidationResult.fail("summon tile occupied");

        boolean adjacentFriendly = false;
        for (TilePos adj : board.getAdjacentOrthogonal(tile)) {
            Unit u = board.getUnitAt(adj).orElse(null);
            if (u != null && u.getOwnerId().equals(ownerId)) {
                adjacentFriendly = true;
                break;
            }
        }
        if (!adjacentFriendly) return ValidationResult.fail("summon must be adjacent to friendly unit");

        if (!state.getPlayer(ownerId).canPay(card.getCost())) return ValidationResult.fail("not enough mana");
        return ValidationResult.ok();
    }

    public ValidationResult validateSpellTarget(GameState<Card> state, String ownerId, SpellCard card, CardTarget target) {
        if (state == null) return ValidationResult.fail("state is null");
        if (card == null) return ValidationResult.fail("card is null");
        if (target == null) return ValidationResult.fail("target is null");

        if (!state.getPlayer(ownerId).canPay(card.getCost())) return ValidationResult.fail("not enough mana");

        TargetSpec spec = card.getTargetSpec();

        if (spec.isNoTarget()) {
            return ValidationResult.ok();
        }

        if (spec.teleportLike()) {
            if (target.getUnitId() == null || target.getTile() == null) return ValidationResult.fail("requires unit+tile target");
            Unit unit = state.getBoard().getUnitById(target.getUnitId()).orElse(null);
            if (unit == null) return ValidationResult.fail("unit not found");
            if (!unit.getOwnerId().equals(ownerId)) return ValidationResult.fail("must target friendly unit");
            if (unit instanceof Avatar) return ValidationResult.fail("cannot target avatar");
            if (!state.getBoard().isInside(target.getTile())) return ValidationResult.fail("tile outside board");
            if (!state.getBoard().isEmpty(target.getTile())) return ValidationResult.fail("tile occupied");

            boolean adjacentFriendly = false;
            for (TilePos adj : state.getBoard().getAdjacentOrthogonal(target.getTile())) {
                Unit u = state.getBoard().getUnitAt(adj).orElse(null);
                if (u != null && u.getOwnerId().equals(ownerId)) { adjacentFriendly = true; break; }
            }
            if (!adjacentFriendly) return ValidationResult.fail("destination must be adjacent to friendly unit");
            return ValidationResult.ok();
        }

        if (spec.unitOnly()) {
            if (target.getUnitId() == null) return ValidationResult.fail("requires unit target");
            Unit unit = state.getBoard().getUnitById(target.getUnitId()).orElse(null);
            if (unit == null) return ValidationResult.fail("unit not found");

            if (spec.friendlyOnly() && !unit.getOwnerId().equals(ownerId)) return ValidationResult.fail("must target friendly unit");
            if (spec.enemyOnly() && unit.getOwnerId().equals(ownerId)) return ValidationResult.fail("must target enemy unit");
            if (!spec.canTargetAvatar() && unit instanceof Avatar) return ValidationResult.fail("cannot target avatar");

            return ValidationResult.ok();
        }

        if (spec.isEmptyTileGlobal()) {
            if (target.getTile() == null) return ValidationResult.fail("requires tile target");
            if (!state.getBoard().isInside(target.getTile())) return ValidationResult.fail("tile outside board");
            if (!state.getBoard().isEmpty(target.getTile())) return ValidationResult.fail("tile occupied");
            return ValidationResult.ok();
        }

        return ValidationResult.fail("unsupported target spec");
    }

    private boolean enemyHasProtector(GameState<Card> state, String actorPlayerId) {
        for (Unit u : state.getBoard().getAllUnits()) {
            if (!u.getOwnerId().equals(actorPlayerId) && u.hasKeyword(UnitKeyword.PROTECTOR) && !(u instanceof Avatar) && !u.isDead()) {
                return true;
            }
        }
        return false;
    }

    private boolean isProvoked(GameState<Card> state, String actorPlayerId, TilePos pos) {
        String enemyId = state.getOpponent(actorPlayerId).getId();
        for (TilePos adj : state.getBoard().getAdjacentOrthogonal(pos)) {
            Unit u = state.getBoard().getUnitAt(adj).orElse(null);
            if (u != null && u.getOwnerId().equals(enemyId) && u.hasKeyword(UnitKeyword.PROVOKE) && !u.isDead()) {
                return true;
            }
        }
        return false;
    }
}