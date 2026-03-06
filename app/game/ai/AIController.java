package game.ai;

import game.card.*;
import game.model.*;
import game.system.action.ActionValidator;
import game.system.action.ValidationResult;

import java.util.*;

/**
 * Very lightweight AI:
 * - try attack best target
 * - else try play a card
 * - else try move closer
 * - else end turn
 *
 * NOTE: This implementation avoids Java 16+ "pattern matching instanceof" syntax
 * for compatibility with typical Eclipse course setups (Java 8/11).
 */
public class AIController {

    public enum StepType { NONE, MOVE, ATTACK, PLAY_CARD, END_TURN }

    public static class StepResult {
        public final StepType type;
        public final String unitId;
        public final TilePos targetPos;
        public final String targetUnitId;
        public final Integer handIndex; // 0-based within Hand snapshot
        public final CardTarget cardTarget;

        private StepResult(StepType type, String unitId, TilePos targetPos, String targetUnitId,
                           Integer handIndex, CardTarget cardTarget) {
            this.type = type;
            this.unitId = unitId;
            this.targetPos = targetPos;
            this.targetUnitId = targetUnitId;
            this.handIndex = handIndex;
            this.cardTarget = cardTarget;
        }

        public static StepResult none() { return new StepResult(StepType.NONE, null, null, null, null, null); }
        public static StepResult move(String unitId, TilePos p) { return new StepResult(StepType.MOVE, unitId, p, null, null, null); }
        public static StepResult attack(String unitId, String targetUnitId) { return new StepResult(StepType.ATTACK, unitId, null, targetUnitId, null, null); }
        public static StepResult playCard(int handIndex, CardTarget t) { return new StepResult(StepType.PLAY_CARD, null, null, null, handIndex, t); }
        public static StepResult endTurn() { return new StepResult(StepType.END_TURN, null, null, null, null, null); }
    }

    private final game.core.GameManager gm;
    private final ActionValidator validator;

    public AIController(game.core.GameManager gm) {
        this.gm = gm;
        this.validator = gm.getActionValidator();
    }

    public StepResult step(game.model.GameState<Card> state) {
        if (state == null) return StepResult.none();
        if (state.isGameOver()) return StepResult.none();
        if (!game.model.GameState.P2.equals(state.getActivePlayerId())) return StepResult.none();

        StepResult atk = tryAttack(state);
        if (atk.type != StepType.NONE) {
            gm.attack(game.model.GameState.P2, atk.unitId, atk.targetUnitId);
            return atk;
        }

        StepResult play = tryPlayCard(state);
        if (play.type != StepType.NONE) {
            gm.playCardFromHand(game.model.GameState.P2, play.handIndex, play.cardTarget);
            return play;
        }

        StepResult mv = tryMove(state);
        if (mv.type != StepType.NONE) {
            gm.moveUnit(game.model.GameState.P2, mv.unitId, mv.targetPos);
            return mv;
        }

        gm.endTurn(game.model.GameState.P2);
        return StepResult.endTurn();
    }

    private StepResult tryAttack(game.model.GameState<Card> state) {
        Board b = state.getBoard();
        Player<Card> p2 = state.getPlayer(game.model.GameState.P2);

        double bestScore = Double.NEGATIVE_INFINITY;
        String bestAttacker = null;
        String bestDefender = null;

        for (Unit u : b.getUnitsByOwner(game.model.GameState.P2)) {
            if (!u.canAttack()) continue;

            // consider all enemy units as potential targets
            for (Unit e : b.getUnitsByOwner(game.model.GameState.P1)) {
                ValidationResult vr = validator.validateAttack(state, game.model.GameState.P2, u.getId(), e.getId());
                if (!vr.isOk()) continue;

                double s = scoreAttack(u, e);
                if (s > bestScore) {
                    bestScore = s;
                    bestAttacker = u.getId();
                    bestDefender = e.getId();
                }
            }

            // consider avatar target as well
            Avatar enemyAvatar = state.getPlayer(game.model.GameState.P1).getAvatar();
            ValidationResult vrA = validator.validateAttack(state, game.model.GameState.P2, u.getId(), enemyAvatar.getId());
            if (vrA.isOk()) {
                double s = scoreAttack(u, enemyAvatar);
                if (s > bestScore) {
                    bestScore = s;
                    bestAttacker = u.getId();
                    bestDefender = enemyAvatar.getId();
                }
            }
        }

        if (bestAttacker == null || bestDefender == null) return StepResult.none();
        return StepResult.attack(bestAttacker, bestDefender);
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

    private StepResult tryPlayCard(game.model.GameState<Card> state) {
        Player<Card> p2 = state.getPlayer(game.model.GameState.P2);
        int mana = p2.getMana();
        List<Card> hand = p2.getHand().snapshot();
        if (hand.isEmpty()) return StepResult.none();

        int bestIndex = -1;
        CardTarget bestTarget = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < hand.size(); i++) {
            Card c = hand.get(i);
            if (c.getCost() > mana) continue;

            if (c instanceof UnitCard) {
                UnitCard uc = (UnitCard) c;
                CardTarget t = pickBestSummonTarget(state, uc);
                if (t == null) continue;

                double score = 10.0 + c.getCost() * 5.0;
                if (score > bestScore) {
                    bestScore = score;
                    bestIndex = i;
                    bestTarget = t;
                }
            } else if (c instanceof SpellCard) {
                SpellCard sc = (SpellCard) c;

                // Portal Step is composite target; skip for now to avoid mis-targeting
                if (sc.getTargetSpec() != null && sc.getTargetSpec().teleportLike()) continue;

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
        return StepResult.playCard(bestIndex, bestTarget);
    }

    private double scoreSpell(game.model.GameState<Card> state, SpellCard sc, CardTarget t) {
        // heuristic only
        String name = sc.getName() == null ? "" : sc.getName().toLowerCase(Locale.ROOT);
        double s = 0.0;

        if (t.getUnitId() != null) {
            Unit u = state.getBoard().getUnitById(t.getUnitId()).orElse(null);
            if (u != null) {
                if (name.contains("true strike") || name.contains("truestrike")) {
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

    private CardTarget pickBestSummonTarget(game.model.GameState<Card> state, UnitCard uc) {
        Avatar enemyAvatar = state.getPlayer(game.model.GameState.P1).getAvatar();
        TilePos enemyPos = enemyAvatar.getPosition();

        TilePos best = null;
        int bestDist = Integer.MAX_VALUE;

        for (int x = 0; x < state.getRules().getBoardWidth(); x++) {
            for (int y = 0; y < state.getRules().getBoardHeight(); y++) {
                TilePos p = new TilePos(x, y);
                ValidationResult vr = validator.validateSummon(state, game.model.GameState.P2, uc, p);
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

    private CardTarget pickBestSpellTarget(game.model.GameState<Card> state, SpellCard sc) {
        double bestScore = Double.NEGATIVE_INFINITY;
        CardTarget best = null;

        // unit targets
        for (Unit u : state.getBoard().getAllUnits()) {
            CardTarget t = CardTarget.unit(u.getId());
            ValidationResult vr = validator.validateSpellTarget(state, game.model.GameState.P2, sc, t);
            if (!vr.isOk()) continue;

            double score = scoreSpell(state, sc, t);
            if (score > bestScore) {
                bestScore = score;
                best = t;
            }
        }
        return best;
    }

    private StepResult tryMove(game.model.GameState<Card> state) {
        Board b = state.getBoard();
        Avatar enemyAvatar = state.getPlayer(game.model.GameState.P1).getAvatar();
        TilePos enemyPos = enemyAvatar.getPosition();

        String bestUnit = null;
        TilePos bestPos = null;
        int bestDist = Integer.MAX_VALUE;

        for (Unit u : b.getUnitsByOwner(game.model.GameState.P2)) {
        	if (!u.canMove()) continue;

        	 List<TilePos> moves = validator.getReachableTiles(b, u);
            for (TilePos p : moves) {
                int dist = b.manhattanDistance(p, enemyPos);
                if (dist < bestDist) {
                    bestDist = dist;
                    bestUnit = u.getId();
                    bestPos = p;
                }
            }
        }

        if (bestUnit == null || bestPos == null) return StepResult.none();
        return StepResult.move(bestUnit, bestPos);
    }
}