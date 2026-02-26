package game.system.resolve;

import game.card.Card;
import game.model.GameState;
import game.model.Unit;
import game.model.UnitKeyword;

public class CombatResolver {
    private final DeathResolver deathResolver;

    public CombatResolver(DeathResolver deathResolver) {
        this.deathResolver = deathResolver;
    }

    /**
     * Combat resolution order (non-recursive):
     * 1) Defender takes primary damage.
     * 2) Defender counterattacks only if survives, counterUsed=false, and attacker is in defender range.
     * 3) Consume attacker's attack action.
     * 4) Attack-first-loses-move (Sniper exception).
     * 5) Remove dead units and check avatar death.
     */
    public void resolveAttack(GameState<Card> state, String attackerId, String defenderId) {
        Unit attacker = state.getBoard().getUnitById(attackerId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown attacker: " + attackerId));
        Unit defender = state.getBoard().getUnitById(defenderId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown defender: " + defenderId));

        // 1) Primary damage
        defender.damage(attacker.getAttack());

        // 2) Counterattack (once, non-recursive)
        boolean defenderSurvives = !defender.isDead();
        boolean inDefenderRange = state.getBoard().manhattanDistance(attacker.getPosition(), defender.getPosition())
                <= defender.getAttackRange();
        if (defenderSurvives && !defender.isCounterUsed() && inDefenderRange) {
            attacker.damage(defender.getAttack());
            defender.setCounterUsed(true);
        }

        // 3) Consume attack action
        attacker.setAttackRemaining(Math.max(0, attacker.getAttackRemaining() - 1));

        // 4) Attack-first-loses-move (Sniper exception)
        if (!attacker.hasMoved() && !attacker.hasKeyword(UnitKeyword.SNIPER)) {
            attacker.setMoveRemaining(0);
        }

        // 5) Death removal / game over
        deathResolver.removeDeadUnitsAndCheckGameOver(state);
    }
}
