package game.system.action;

import game.card.*;
import game.model.*;

import java.util.List;

public class ActionValidator {
    private final ReachabilityService reachabilityService;

    public ActionValidator(ReachabilityService reachabilityService) {
        this.reachabilityService = reachabilityService;
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
        if (unit.getMoveRange() <= 0) return ValidationResult.fail("unit cannot move");

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

        // Protector rule: enemy avatar cannot be attacked if enemy protector unit exists.
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
            if (u != null && ownerId.equals(u.getOwnerId())) {
                adjacentFriendly = true;
                break;
            }
        }
        if (!adjacentFriendly) {
            return ValidationResult.fail("summon tile must be orthogonally adjacent to friendly avatar/unit");
        }

        return ValidationResult.ok();
    }

    public ValidationResult validateSpellTarget(GameState<Card> state, String ownerId, SpellCard card, CardTarget target) {
        if (state == null) return ValidationResult.fail("state is null");
        if (card == null) return ValidationResult.fail("spell card is null");
        if (target == null) return ValidationResult.fail("target is null");

        TargetSpec spec = card.getTargetSpec();
        Board board = state.getBoard();

        if (spec.isTeleportLike()) {
            if (target.getUnitId() == null || target.getTile() == null) {
                return ValidationResult.fail("Portal Step requires source unit and destination tile");
            }
            Unit source = board.getUnitById(target.getUnitId()).orElse(null);
            if (source == null) return ValidationResult.fail("source unit not found");
            if (!ownerId.equals(source.getOwnerId())) return ValidationResult.fail("Portal Step requires friendly unit");
            if (source instanceof Avatar) return ValidationResult.fail("Portal Step cannot target avatar");
            if (!board.isInside(target.getTile())) return ValidationResult.fail("destination outside board");
            if (!board.isEmpty(target.getTile())) return ValidationResult.fail("destination occupied");
            // destination follows summon adjacency rule
            return validateSummonAdjacencyOnly(state, ownerId, target.getTile());
        }

        if (spec.isAllowTile()) {
            if (target.getTile() == null) return ValidationResult.fail("tile target required");
            if (!board.isInside(target.getTile())) return ValidationResult.fail("tile target outside board");
            if (spec.isRequiresEmptyTile() && !board.isEmpty(target.getTile())) {
                return ValidationResult.fail("tile target must be empty");
            }
            return ValidationResult.ok();
        }

        if (target.getUnitId() == null) return ValidationResult.fail("unit target required");
        Unit unit = board.getUnitById(target.getUnitId()).orElse(null);
        if (unit == null) return ValidationResult.fail("target unit not found");

        boolean friendly = ownerId.equals(unit.getOwnerId());
        if (friendly && !spec.isAllowFriendly()) return ValidationResult.fail("friendly target not allowed");
        if (!friendly && !spec.isAllowEnemy()) return ValidationResult.fail("enemy target not allowed");
        if (unit instanceof Avatar && !spec.isAllowAvatar()) return ValidationResult.fail("avatar target not allowed");
        if (!(unit instanceof Avatar) && !spec.isAllowUnit()) return ValidationResult.fail("unit target not allowed");

        if (spec.getRange() >= 0) {
            Avatar casterAvatar = state.getPlayer(ownerId).getAvatar();
            int dist = board.manhattanDistance(casterAvatar.getPosition(), unit.getPosition());
            if (dist > spec.getRange()) return ValidationResult.fail("spell target out of range");
        }

        return ValidationResult.ok();
    }

    public List<TilePos> getReachableTiles(Board board, Unit unit) {
        return reachabilityService.reachableTiles(board, unit);
    }

    public boolean enemyHasProtector(GameState<Card> state, String attackerPlayerId) {
        String enemyId = state.getOpponent(attackerPlayerId).getId();
        for (Unit unit : state.getBoard().getUnitsByOwner(enemyId)) {
            if (!(unit instanceof Avatar) && !unit.isDead() && unit.hasKeyword(UnitKeyword.PROTECTOR)) {
                return true;
            }
        }
        return false;
    }

    private ValidationResult validateSummonAdjacencyOnly(GameState<Card> state, String ownerId, TilePos tile) {
        Board board = state.getBoard();
        if (!board.isInside(tile)) return ValidationResult.fail("tile outside board");
        if (!board.isEmpty(tile)) return ValidationResult.fail("tile occupied");
        for (TilePos adj : board.getAdjacentOrthogonal(tile)) {
            Unit u = board.getUnitAt(adj).orElse(null);
            if (u != null && ownerId.equals(u.getOwnerId())) return ValidationResult.ok();
        }
        return ValidationResult.fail("destination must be adjacent to friendly avatar/unit");
    }
}
