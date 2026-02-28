package game.ai;

import game.card.*;
import game.core.GameManager;
import game.model.*;
import game.system.action.ActionValidator;
import game.system.action.ReachabilityService;
import game.system.action.ValidationResult;

import java.util.Locale;
import java.util.List;

/**
 * AIController
 *
 * - Generates ONLY legal actions (ActionValidator)
 * - Executes ONLY via GameManager
 * - Does exactly ONE action per step() call
 */
public class AIController {

    public enum StepType { MOVE, ATTACK, PLAY_CARD, END_TURN, NONE }

    public static final class StepResult {
        public final StepType type;
        public final String unitId;        // mover/attacker
        public final String targetUnitId;  // defender
        public StepResult(StepType type, String unitId, String targetUnitId) {
            this.type = type;
            this.unitId = unitId;
            this.targetUnitId = targetUnitId;
        }
        public static StepResult none() { return new StepResult(StepType.NONE, null, null); }
        public static StepResult move(String unitId) { return new StepResult(StepType.MOVE, unitId, null); }
        public static StepResult attack(String attackerId, String defenderId) { return new StepResult(StepType.ATTACK, attackerId, defenderId); }
        public static StepResult play() { return new StepResult(StepType.PLAY_CARD, null, null); }
        public static StepResult endTurn() { return new StepResult(StepType.END_TURN, null, null); }
    }

    private final GameManager gm;
    private final ActionValidator validator;

    public AIController(GameManager gm) {
        this.gm = gm;
        this.validator = new ActionValidator(new ReachabilityService());
    }

    /** Execute exactly ONE AI action if possible. */
    public StepResult step(GameState<Card> state) {
        if (state == null || state.isGameOver()) return StepResult.none();
        if (!GameState.P2.equals(state.getActivePlayerId())) return StepResult.none();

        // 1) Prefer attack if possible
        StepResult atk = tryBestAttack(state);
        if (atk.type == StepType.ATTACK) return atk;

        // 2) Try play a card
        StepResult play = tryPlayCard(state);
        if (play.type == StepType.PLAY_CARD) return play;

        // 3) Otherwise move
        StepResult mv = tryMove(state);
        if (mv.type == StepType.MOVE) return mv;

        // 4) Nothing to do
        return StepResult.none();
    }

    private StepResult tryBestAttack(GameState<Card> state) {
        List<Unit> myUnits = state.getBoard().getUnitsByOwner(GameState.P2);

        double bestScore = Double.NEGATIVE_INFINITY;
        String bestA = null;
        String bestD = null;

        for (Unit a : myUnits) {
            if (a.isDead()) continue;
            if (a.getFrozenTurns() > 0) continue;
            if (a.getAttackRemaining() <= 0) continue;

            for (Unit d : state.getBoard().getUnitsByOwner(GameState.P1)) {
                if (d.isDead()) continue;

                ValidationResult vr = validator.validateAttack(state, GameState.P2, a.getId(), d.getId());
                if (!vr.isOk()) continue;

                double score = scoreAttack(a, d);
                if (score > bestScore) {
                    bestScore = score;
                    bestA = a.getId();
                    bestD = d.getId();
                }
            }
        }

        if (bestA == null) return StepResult.none();

        ValidationResult exec = gm.attack(GameState.P2, bestA, bestD);
        if (!exec.isOk()) return StepResult.none();
        return StepResult.attack(bestA, bestD);
    }

    private double scoreAttack(Unit attacker, Unit defender) {
        int dmg = attacker.getAttack();
        boolean lethal = defender.getHp() - dmg <= 0;

        double s = 0.0;
        if (defender instanceof Avatar) s += 50.0;   // bias to hit avatar
        if (lethal) s += 100.0;                     // prefer lethal
        s += Math.min(20, dmg) * 2.0;               // prefer higher dmg
        s += Math.max(0, 10 - defender.getHp());    // prefer low hp targets
        return s;
    }

    private StepResult tryPlayCard(GameState<Card> state) {
        Player<Card> p2 = state.getPlayer(GameState.P2);
        int mana = p2.getMana();
        List<Card> hand = p2.getHand().snapshot();
        if (hand.isEmpty()) return StepResult.none();

        int bestIndex = -1;
        CardTarget bestTarget = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < hand.size(); i++) {
            Card c = hand.get(i);
            if (c.getCost() > mana) continue;

            if (c instanceof UnitCard uc) {
                CardTarget t = pickBestSummonTarget(state, uc);
                if (t == null) continue;
                double score = 10.0 + c.getCost() * 5.0;
                if (score > bestScore) {
                    bestScore = score;
                    bestIndex = i;
                    bestTarget = t;
                }
            } else if (c instanceof SpellCard sc) {
                // Portal Step is composite target; skip for now to avoid mis-targeting
                if (sc.getTargetSpec() != null && sc.getTargetSpec().isTeleportLike()) continue;

                CardTarget t = pickBestSpellTarget(state, sc);
                if (t == null) continue;
                double score = scoreSpell(state, sc, t);
                if (score > bestScore) {
                    bestScore = score;
                    bestIndex = i;
                    bestTarget = t;
                }
            }
        }

        if (bestIndex < 0 || bestTarget == null) return StepResult.none();

        ValidationResult exec = gm.playCardFromHand(GameState.P2, bestIndex, bestTarget);
        if (!exec.isOk()) return StepResult.none();
        return StepResult.play();
    }

    private double scoreSpell(GameState<Card> state, SpellCard sc, CardTarget t) {
        String name = sc.getName().toLowerCase(Locale.ROOT);
        double s = 5.0 + sc.getCost();

        if (t.getUnitId() != null) {
            Unit u = state.getBoard().getUnitById(t.getUnitId()).orElse(null);
            if (u != null) {
                if (name.contains("true strike")) {
                    int dmg = 2;
                    if (u.getHp() - dmg <= 0) s += 120.0;
                    if (u instanceof Avatar) s += 60.0;
                    s += (10 - u.getHp());
                }
                if (name.contains("beam shock")) {
                    s += u.getAttack() * 10.0;
                }
                if (name.contains("sundrop")) {
                    int missing = Math.max(0, u.getMaxHp() - u.getHp());
                    s += missing * 15.0;
                }
                if (name.contains("smite")) {
                    s += u.getAttack() * 8.0 + u.getHp() * 3.0;
                }
            }
        }
        return s;
    }

    private CardTarget pickBestSummonTarget(GameState<Card> state, UnitCard uc) {
        Avatar enemyAvatar = state.getPlayer(GameState.P1).getAvatar();
        TilePos enemyPos = enemyAvatar.getPosition();

        TilePos best = null;
        int bestDist = Integer.MAX_VALUE;

        for (int x = 0; x < state.getRules().getBoardWidth(); x++) {
            for (int y = 0; y < state.getRules().getBoardHeight(); y++) {
                TilePos p = new TilePos(x, y);
                ValidationResult vr = validator.validateSummon(state, GameState.P2, uc, p);
                if (!vr.isOk()) continue;

                int dist = state.getBoard().manhattanDistance(p, enemyPos);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = p;
                }
            }
        }
        return best == null ? null : CardTarget.tile(best);
    }

    private CardTarget pickBestSpellTarget(GameState<Card> state, SpellCard sc) {
        double bestScore = Double.NEGATIVE_INFINITY;
        CardTarget best = null;

        // unit targets
        for (Unit u : state.getBoard().getAllUnits()) {
            CardTarget t = CardTarget.unit(u.getId());
            ValidationResult vr = validator.validateSpellTarget(state, GameState.P2, sc, t);
            if (!vr.isOk()) continue;

            double s = scoreSpell(state, sc, t);
            if (s > bestScore) {
                bestScore = s;
                best = t;
            }
        }

        // tile targets (optional)
        for (int x = 0; x < state.getRules().getBoardWidth(); x++) {
            for (int y = 0; y < state.getRules().getBoardHeight(); y++) {
                TilePos p = new TilePos(x, y);
                CardTarget t = CardTarget.tile(p);
                ValidationResult vr = validator.validateSpellTarget(state, GameState.P2, sc, t);
                if (!vr.isOk()) continue;

                double s = 1.0;
                if (s > bestScore) {
                    bestScore = s;
                    best = t;
                }
            }
        }

        return best;
    }

    private StepResult tryMove(GameState<Card> state) {
        Avatar enemyAvatar = state.getPlayer(GameState.P1).getAvatar();
        TilePos enemyPos = enemyAvatar.getPosition();

        List<Unit> myUnits = state.getBoard().getUnitsByOwner(GameState.P2);

        String bestUnit = null;
        TilePos bestTile = null;
        int bestImprove = 0;

        for (Unit u : myUnits) {
            if (u.isDead()) continue;
            if (u.getFrozenTurns() > 0) continue;
            if (u.getMoveRemaining() <= 0) continue;
            if (u.getMoveRange() <= 0) continue;

            int curDist = state.getBoard().manhattanDistance(u.getPosition(), enemyPos);

            List<TilePos> reach = gm.getReachableTiles(u.getId());
            for (TilePos t : reach) {
                ValidationResult vr = validator.validateMove(state, GameState.P2, u.getId(), t);
                if (!vr.isOk()) continue;

                int newDist = state.getBoard().manhattanDistance(t, enemyPos);
                int improve = curDist - newDist;
                if (improve > bestImprove) {
                    bestImprove = improve;
                    bestUnit = u.getId();
                    bestTile = t;
                }
            }
        }

        if (bestUnit == null || bestTile == null) return StepResult.none();

        ValidationResult exec = gm.moveUnit(GameState.P2, bestUnit, bestTile);
        if (!exec.isOk()) return StepResult.none();
        return StepResult.move(bestUnit);
    }
}