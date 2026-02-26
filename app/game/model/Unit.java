package game.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public class Unit {
    private final String id;
    private final String ownerId;
    private final String name;

    private int attack;
    private int hp;
    private int maxHp;

    private int moveRange;
    private int attackRange;

    private TilePos position;

    // Per-turn/action state
    private int moveRemaining;      // action count (0 or 1)
    private int attackRemaining;    // 0,1,2
    private boolean hasMoved;
    private boolean counterUsed;
    private int frozenTurns;
    private boolean newlySummonedThisTurn;

    private final EnumSet<UnitKeyword> keywords = EnumSet.noneOf(UnitKeyword.class);

    public Unit(String id, String ownerId, String name, int attack, int hp, int moveRange, int attackRange, TilePos position) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.attack = attack;
        this.hp = hp;
        this.maxHp = hp;
        this.moveRange = moveRange;
        this.attackRange = attackRange;
        this.position = position;
        this.moveRemaining = 0;
        this.attackRemaining = 0;
        this.hasMoved = false;
        this.counterUsed = false;
        this.frozenTurns = 0;
        this.newlySummonedThisTurn = false;
    }

    public String getId() { return id; }
    public String getOwnerId() { return ownerId; }
    public String getName() { return name; }

    public int getAttack() { return attack; }
    public void setAttack(int attack) { this.attack = attack; }

    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }
    public void setMaxHp(int maxHp) { this.maxHp = maxHp; }

    public void damage(int amount) { this.hp -= Math.max(0, amount); }
    public void heal(int amount) { this.hp = Math.min(maxHp, this.hp + Math.max(0, amount)); }

    public boolean isDead() { return hp <= 0; }

    public int getMoveRange() { return moveRange; }
    public void setMoveRange(int moveRange) { this.moveRange = moveRange; }

    public int getAttackRange() { return attackRange; }
    public void setAttackRange(int attackRange) { this.attackRange = attackRange; }

    public TilePos getPosition() { return position; }
    public void setPosition(TilePos position) { this.position = position; }

    public int getMoveRemaining() { return moveRemaining; }
    public void setMoveRemaining(int moveRemaining) { this.moveRemaining = Math.max(0, moveRemaining); }

    public int getAttackRemaining() { return attackRemaining; }
    public void setAttackRemaining(int attackRemaining) { this.attackRemaining = Math.max(0, attackRemaining); }

    public boolean hasMoved() { return hasMoved; }
    public void setHasMoved(boolean hasMoved) { this.hasMoved = hasMoved; }

    public boolean isCounterUsed() { return counterUsed; }
    public void setCounterUsed(boolean counterUsed) { this.counterUsed = counterUsed; }

    public int getFrozenTurns() { return frozenTurns; }
    public void setFrozenTurns(int frozenTurns) { this.frozenTurns = Math.max(0, frozenTurns); }
    public void addFrozenTurns(int turns) { this.frozenTurns = Math.max(this.frozenTurns, Math.max(0, turns)); }

    public boolean isNewlySummonedThisTurn() { return newlySummonedThisTurn; }
    public void setNewlySummonedThisTurn(boolean newlySummonedThisTurn) { this.newlySummonedThisTurn = newlySummonedThisTurn; }

    public boolean hasKeyword(UnitKeyword keyword) { return keywords.contains(keyword); }
    public void addKeyword(UnitKeyword keyword) { keywords.add(keyword); }
    public void removeKeyword(UnitKeyword keyword) { keywords.remove(keyword); }
    public Set<UnitKeyword> getKeywords() { return Collections.unmodifiableSet(keywords); }

    public void markSummonedThisTurn() {
        this.newlySummonedThisTurn = true;
        this.moveRemaining = 0;
        this.attackRemaining = 0;
    }

    /**
     * Start-of-turn refresh (owner's turn only).
     * Frozen units lose actions this turn, and frozenTurns decreases by 1.
     */
    public void refreshForOwnerTurn() {
        this.counterUsed = false;
        this.hasMoved = false;
        this.newlySummonedThisTurn = false; // no longer "newly summoned" once owner's next turn starts

        if (frozenTurns > 0) {
            frozenTurns -= 1;
            moveRemaining = 0;
            attackRemaining = 0;
            return;
        }

        moveRemaining = 1;
        attackRemaining = hasKeyword(UnitKeyword.ATTACK_TWICE) ? 2 : 1;
    }

    @Override
    public String toString() {
        return name + "[" + id + "]" +
                " owner=" + ownerId +
                " hp=" + hp + "/" + maxHp +
                " atk=" + attack +
                " pos=" + position +
                " moveRem=" + moveRemaining +
                " atkRem=" + attackRemaining +
                " frozen=" + frozenTurns +
                " keywords=" + keywords;
    }
}
