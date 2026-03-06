package game.card;

import game.card.effect.*;
import game.model.UnitKeyword;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Function;

/**
 * Builds the fixed 20-card decks (two copies of each of the 10 specified cards) for Phase 2.
 *
 * Player 1 (Human): Abyssian Swarm
 * Player 2 (AI): Lyonar Generalist
 *
 * Source: Y25-26-Decks.pdf (course-provided deck specification).
 */
public final class FixedStoryDeckFactory {

    private FixedStoryDeckFactory() {}

    public static List<Card> buildDeckFor(String playerId) {
        if (game.model.GameState.P1.equals(playerId)) {
            return buildAbyssianSwarm();
        }
        return buildLyonarGeneralist();
    }

    private static UnitTemplate wraithlingTemplate() {
        return UnitTemplate.simple("Wraithling", 1, 1, 2, 1, EnumSet.noneOf(UnitKeyword.class));
    }

    private static List<Card> buildAbyssianSwarm() {
        List<Card> deck = new ArrayList<>(20);
        UnitTemplate wraithling = wraithlingTemplate();

        addCopies(deck, "C_BAD_OMEN", 2, id -> new UnitCard(id, "Bad Omen", 0,
                UnitTemplate.withDeathwatch("Bad Omen", 0, 1, 2, 1, EnumSet.noneOf(UnitKeyword.class),
                        List.of(new BuffSelfAttackOnDeathwatchEffect(1))
                )));

        addCopies(deck, "C_GLOOM_CHASER", 2, id -> new UnitCard(id, "Gloom Chaser", 2,
                UnitTemplate.withOnSummon("Gloom Chaser", 3, 1, 2, 1, EnumSet.noneOf(UnitKeyword.class),
                        List.of(new SummonWraithlingBehindOnSummonEffect(wraithling))
                )));

        addCopies(deck, "C_ROCK_PULVERISER", 2, id -> new UnitCard(id, "Rock Pulveriser", 2,
                UnitTemplate.simple("Rock Pulveriser", 1, 4, 2, 1, EnumSet.of(UnitKeyword.PROVOKE))));

        addCopies(deck, "C_SHADOW_WATCHER", 2, id -> new UnitCard(id, "Shadow Watcher", 3,
                UnitTemplate.withDeathwatch("Shadow Watcher", 3, 2, 2, 1, EnumSet.noneOf(UnitKeyword.class),
                        List.of(new BuffSelfOnDeathwatchEffect(1, 1))
                )));

        addCopies(deck, "C_NIGHTSORROW_ASSASSIN", 2, id -> new UnitCard(id, "Nightsorrow Assassin", 3,
                UnitTemplate.withOnSummon("Nightsorrow Assassin", 4, 2, 2, 1, EnumSet.noneOf(UnitKeyword.class),
                        List.of(new DestroyAdjacentWoundedEnemyOnSummonEffect())
                )));

        addCopies(deck, "C_BLOODMOON_PRIESTESS", 2, id -> new UnitCard(id, "Bloodmoon Priestess", 4,
                UnitTemplate.withDeathwatch("Bloodmoon Priestess", 3, 3, 2, 1, EnumSet.noneOf(UnitKeyword.class),
                        List.of(new SummonWraithlingAdjacentOnDeathwatchEffect(wraithling))
                )));

        addCopies(deck, "C_SHADOWDANCER", 2, id -> new UnitCard(id, "Shadowdancer", 5,
                UnitTemplate.withDeathwatch("Shadowdancer", 5, 4, 2, 1, EnumSet.noneOf(UnitKeyword.class),
                        List.of(new ShadowdancerDeathwatchEffect())
                )));

        addCopies(deck, "S_HORN_OF_THE_FORSAKEN", 2, id -> new SpellCard(id, "Horn of the Forsaken", 1,
                TargetSpec.none(),
                new EquipHornOfTheForsakenEffect(3)
        ));

        addCopies(deck, "S_WRAITHLING_SWARM", 2, id -> new SpellCard(id, "Wraithling Swarm", 3,
                TargetSpec.emptyTileGlobal(),
                new SummonThreeWraithlingsEffect(wraithling)
        ));

        addCopies(deck, "S_DARK_TERMINUS", 2, id -> new SpellCard(id, "Dark Terminus", 4,
                TargetSpec.enemyUnitNonAvatar(),
                new DarkTerminusEffect(wraithling)
        ));

        return deck;
    }

    private static List<Card> buildLyonarGeneralist() {
        List<Card> deck = new ArrayList<>(20);

        addCopies(deck, "C_SWAMP_ENTANGLER", 2, id -> new UnitCard(id, "Swamp Entangler", 1,
                UnitTemplate.simple("Swamp Entangler", 0, 3, 2, 1, EnumSet.of(UnitKeyword.PROVOKE))
        ));

        addCopies(deck, "C_SILVERGUARD_SQUIRE", 2, id -> new UnitCard(id, "Silverguard Squire", 1,
                UnitTemplate.withOnSummon("Silverguard Squire", 1, 1, 2, 1, EnumSet.noneOf(UnitKeyword.class),
                        List.of(new BuffAdjacentUnitNearAvatarOnSummonEffect())
                )
        ));

        addCopies(deck, "C_SKYROCK_GOLEM", 2, id -> new UnitCard(id, "Skyrock Golem", 2,
                UnitTemplate.simple("Skyrock Golem", 4, 2, 2, 1, EnumSet.noneOf(UnitKeyword.class))
        ));

        addCopies(deck, "C_SABERSPINE_TIGER", 2, id -> new UnitCard(id, "Saberspine Tiger", 3,
                UnitTemplate.simple("Saberspine Tiger", 3, 2, 2, 1, EnumSet.of(UnitKeyword.RUSH))
        ));

        addCopies(deck, "C_SILVERGUARD_KNIGHT", 2, id -> new UnitCard(id, "Silverguard Knight", 3,
                new UnitTemplate(
                        "Silverguard Knight",
                        1, 5, 2, 1,
                        EnumSet.of(UnitKeyword.PROVOKE, UnitKeyword.ZEAL),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(new GainAttackOnOwnerAvatarDamagedEffect(2))
                )
        ));

        addCopies(deck, "C_YOUNG_FLAMEWING", 2, id -> new UnitCard(id, "Young Flamewing", 4,
                UnitTemplate.simple("Young Flamewing", 5, 4, 2, 1, EnumSet.of(UnitKeyword.FLYING))
        ));

        addCopies(deck, "C_IRONCLIFFE_GUARDIAN", 2, id -> new UnitCard(id, "Ironcliffe Guardian", 5,
                UnitTemplate.simple("Ironcliffe Guardian", 3, 10, 2, 1, EnumSet.of(UnitKeyword.PROVOKE))
        ));

        addCopies(deck, "S_SUNDROP_ELIXIR", 2, id -> new SpellCard(id, "Sundrop Elixir", 1,
                TargetSpec.friendlyUnitAny(),
                new HealEffect(4)
        ));

        addCopies(deck, "S_TRUE_STRIKE", 2, id -> new SpellCard(id, "True Strike", 1,
                TargetSpec.enemyUnitAny(),
                new DamageEffect(2)
        ));

        addCopies(deck, "S_BEAM_SHOCK", 2, id -> new SpellCard(id, "Beam Shock", 0,
                TargetSpec.enemyUnitNonAvatar(),
                new FreezeEffect(1)
        ));

        return deck;
    }

    private static void addCopies(List<Card> deck, String baseId, int copies, Function<String, Card> factory) {
        for (int i = 1; i <= copies; i++) {
            deck.add(factory.apply(baseId + "_" + i));
        }
    }
}