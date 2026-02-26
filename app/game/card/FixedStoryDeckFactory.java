package game.card;

import game.card.effect.*;
import game.model.UnitKeyword;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

/**
 * Fixed 30-card deck used by GameManager for both players.
 *
 * 说明：
 * - 结构与规则来自你们 class list / process flow 中已经确认的机制（UnitCard/SpellCard、Portal Step、Frozen、Wraithling Swarm 等）。
 * - 具体卡名/数值在“story card 未完全给出最终文本”的情况下进行了可运行化补全。
 * - 其中 Medic / Storm Mage / Assassin / Paladin / Ogre 的 on-summon 效果按 class list 示例实现。
 */
public final class FixedStoryDeckFactory {
    private FixedStoryDeckFactory() {}

    public static List<Card> buildDeckFor(String ownerId) {
        List<Card> deck = new ArrayList<>();

        // ---- Unit templates ----
        UnitTemplate footman = UnitTemplate.simple("Footman", 2, 3, 2, 1, EnumSet.noneOf(UnitKeyword.class));
        UnitTemplate guardian = UnitTemplate.simple("Guardian", 1, 5, 1, 1, EnumSet.of(UnitKeyword.PROTECTOR));
        UnitTemplate sniper = UnitTemplate.simple("Sniper", 2, 2, 2, 2, EnumSet.of(UnitKeyword.SNIPER));
        UnitTemplate duelist = UnitTemplate.simple("Duelist", 1, 3, 2, 1, EnumSet.of(UnitKeyword.ATTACK_TWICE));
        UnitTemplate lancer = UnitTemplate.simple("Lancer", 2, 3, 3, 1, EnumSet.noneOf(UnitKeyword.class));
        UnitTemplate colossus = UnitTemplate.simple("Colossus", 4, 7, 1, 1, EnumSet.noneOf(UnitKeyword.class));
        UnitTemplate wraithling = UnitTemplate.simple("Wraithling", 1, 1, 2, 1, EnumSet.noneOf(UnitKeyword.class));

        UnitTemplate medic = new UnitTemplate(
                "Medic", 1, 3, 2, 1, EnumSet.noneOf(UnitKeyword.class),
                Collections.singletonList(new HealOwnerAvatarOnSummonEffect(3))
        );
        UnitTemplate stormMage = new UnitTemplate(
                "Storm Mage", 2, 3, 2, 2, EnumSet.noneOf(UnitKeyword.class),
                Collections.singletonList(new StormMageAoeOnSummonEffect(1))
        );
        UnitTemplate assassin = new UnitTemplate(
                "Assassin", 3, 2, 2, 1, EnumSet.noneOf(UnitKeyword.class),
                Collections.singletonList(new AssassinStrikeOnSummonEffect(2))
        );
        UnitTemplate paladin = new UnitTemplate(
                "Paladin", 2, 4, 2, 1, EnumSet.noneOf(UnitKeyword.class),
                Collections.singletonList(new PaladinBuffAdjacentOnSummonEffect(1, 1))
        );
        UnitTemplate ogre = new UnitTemplate(
                "Ogre", 5, 6, 1, 1, EnumSet.noneOf(UnitKeyword.class),
                Collections.singletonList(new DamageOwnerAvatarOnSummonEffect(2))
        );

        // ---- 18 unit cards ----
        addCopies(deck, ownerId, "footman", "Footman", 1, 4, c -> new UnitCard(c.id, c.name, c.cost, footman));
        addCopies(deck, ownerId, "guardian", "Guardian", 2, 2, c -> new UnitCard(c.id, c.name, c.cost, guardian));
        addCopies(deck, ownerId, "sniper", "Sniper", 2, 2, c -> new UnitCard(c.id, c.name, c.cost, sniper));
        addCopies(deck, ownerId, "duelist", "Duelist", 3, 2, c -> new UnitCard(c.id, c.name, c.cost, duelist));
        addCopies(deck, ownerId, "lancer", "Lancer", 2, 2, c -> new UnitCard(c.id, c.name, c.cost, lancer));
        addCopies(deck, ownerId, "colossus", "Colossus", 4, 1, c -> new UnitCard(c.id, c.name, c.cost, colossus));
        addCopies(deck, ownerId, "medic", "Medic", 2, 1, c -> new UnitCard(c.id, c.name, c.cost, medic));
        addCopies(deck, ownerId, "storm_mage", "Storm Mage", 3, 1, c -> new UnitCard(c.id, c.name, c.cost, stormMage));
        addCopies(deck, ownerId, "assassin", "Assassin", 3, 1, c -> new UnitCard(c.id, c.name, c.cost, assassin));
        addCopies(deck, ownerId, "paladin", "Paladin", 3, 1, c -> new UnitCard(c.id, c.name, c.cost, paladin));
        addCopies(deck, ownerId, "ogre", "Ogre", 4, 1, c -> new UnitCard(c.id, c.name, c.cost, ogre));

        // ---- 12 spell cards ----
        addCopies(deck, ownerId, "true_strike", "True Strike", 1, 3,
                c -> new SpellCard(c.id, c.name, c.cost, TargetSpec.enemyUnitOrAvatarInRange(4), new DamageEffect(2)));
        addCopies(deck, ownerId, "sundrop_elixir", "Sundrop Elixir", 1, 2,
                c -> new SpellCard(c.id, c.name, c.cost, TargetSpec.friendlyUnitOrAvatarInRange(4), new HealEffect(2)));
        addCopies(deck, ownerId, "beam_shock", "Beam Shock", 1, 2,
                c -> new SpellCard(c.id, c.name, c.cost, TargetSpec.enemyUnitOnlyInRange(4), new FreezeEffect(1)));
        addCopies(deck, ownerId, "portal_step", "Portal Step", 1, 2,
                c -> new PortalStepSpellCard(c.id, c.name, c.cost));
        addCopies(deck, ownerId, "wraithling_swarm", "Wraithling Swarm", 2, 2,
                c -> new SpellCard(c.id, c.name, c.cost, TargetSpec.emptyTileGlobal(), new SummonUnitEffect(wraithling)));
        addCopies(deck, ownerId, "smite", "Smite", 2, 1,
                c -> new SpellCard(c.id, c.name, c.cost, TargetSpec.enemyUnitOnlyInRange(4), new DestroyUnitEffect()));

        if (deck.size() != 30) {
            throw new IllegalStateException("Fixed deck must contain 30 cards, but got " + deck.size());
        }
        return deck;
    }

    private interface CardBuilder {
        Card build(CardSpec spec);
    }

    private static final class CardSpec {
        final String id;
        final String name;
        final int cost;
        CardSpec(String id, String name, int cost) {
            this.id = id;
            this.name = name;
            this.cost = cost;
        }
    }

    private static void addCopies(List<Card> deck, String ownerId, String key, String name, int cost, int copies, CardBuilder builder) {
        for (int i = 1; i <= copies; i++) {
            String id = ownerId + "_" + key + "_" + i;
            deck.add(builder.build(new CardSpec(id, name, cost)));
        }
    }
}
