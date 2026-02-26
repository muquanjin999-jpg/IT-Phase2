package game.card;

import game.card.effect.OnSummonEffect;
import game.model.TilePos;
import game.model.Unit;
import game.model.UnitKeyword;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

public final class UnitTemplate {
    private final String name;
    private final int attack;
    private final int hp;
    private final int moveRange;
    private final int attackRange;
    private final EnumSet<UnitKeyword> keywords;
    private final List<OnSummonEffect> onSummonEffects;

    public UnitTemplate(
            String name,
            int attack,
            int hp,
            int moveRange,
            int attackRange,
            EnumSet<UnitKeyword> keywords,
            List<OnSummonEffect> onSummonEffects
    ) {
        this.name = name;
        this.attack = attack;
        this.hp = hp;
        this.moveRange = moveRange;
        this.attackRange = attackRange;
        this.keywords = keywords == null ? EnumSet.noneOf(UnitKeyword.class) : keywords.clone();
        this.onSummonEffects = onSummonEffects == null ? Collections.emptyList() : new ArrayList<>(onSummonEffects);
    }

    public static UnitTemplate simple(String name, int attack, int hp, int moveRange, int attackRange, EnumSet<UnitKeyword> keywords) {
        return new UnitTemplate(name, attack, hp, moveRange, attackRange, keywords, Collections.emptyList());
    }

    public String getName() { return name; }
    public List<OnSummonEffect> getOnSummonEffects() { return Collections.unmodifiableList(onSummonEffects); }

    public Unit create(String unitId, String ownerId, TilePos pos) {
        Unit unit = new Unit(unitId, ownerId, name, attack, hp, moveRange, attackRange, pos);
        for (UnitKeyword kw : keywords) {
            unit.addKeyword(kw);
        }
        return unit;
    }
}
