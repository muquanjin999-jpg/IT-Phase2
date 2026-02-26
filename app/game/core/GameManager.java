package game.core;

import game.card.*;
import game.model.*;
import game.system.action.ActionValidator;
import game.system.action.ReachabilityService;
import game.system.action.ValidationResult;
import game.system.resolve.CombatResolver;
import game.system.resolve.DeathResolver;
import game.system.resolve.MoveResolver;
import game.system.turn.*;

import java.util.List;
import java.util.Random;

public class GameManager {
    private final GameRules rules;
    private final Random random;

    private final ReachabilityService reachabilityService;
    private final ActionValidator actionValidator;
    private final DeathResolver deathResolver;
    private final MoveResolver moveResolver;
    private final CombatResolver combatResolver;
    private final ManaSystem manaSystem;
    private final DrawSystem<Card> drawSystem;
    private final TurnManager<Card> turnManager;
    private final IdGenerator idGenerator;

    private GameState<Card> state;
    private CardPlayContext cardPlayContext;

    public GameManager() {
        this(GameRules.defaultRules(), 42L);
    }

    public GameManager(GameRules rules, long seed) {
        this.rules = rules;
        this.random = new Random(seed);

        this.reachabilityService = new ReachabilityService();
        this.actionValidator = new ActionValidator(reachabilityService);
        this.deathResolver = new DeathResolver();
        this.moveResolver = new MoveResolver();
        this.combatResolver = new CombatResolver(deathResolver);
        this.manaSystem = new ManaSystem(rules.getManaCap());
        this.drawSystem = new DrawSystem<>();
        this.turnManager = new TurnManager<>(manaSystem, drawSystem);
        this.idGenerator = new IdGenerator();
    }

    public GameState<Card> initializeNewGame() {
        Board board = new Board(rules.getBoardWidth(), rules.getBoardHeight());

        Player<Card> p1 = buildPlayer(GameState.P1, "Human", new TilePos(1, 2));
        Player<Card> p2 = buildPlayer(GameState.P2, "AI", new TilePos(7, 2));

        board.placeUnit(p1.getAvatar(), p1.getAvatar().getPosition());
        board.placeUnit(p2.getAvatar(), p2.getAvatar().getPosition());

        GameState<Card> newState = new GameState<>(rules, board, p1, p2);

        // shuffle fixed 30-card decks
        p1.getDeck().shuffle(random);
        p2.getDeck().shuffle(random);

        this.state = newState;
        this.cardPlayContext = new CardPlayContext(state, manaSystem, actionValidator, deathResolver, idGenerator);

        turnManager.initializeMatch(state);
        return state;
    }

    private Player<Card> buildPlayer(String id, String name, TilePos avatarPos) {
        Avatar avatar = new Avatar("avatar-" + id, id, name + " Avatar", rules.getAvatarHp(), avatarPos);
        Deck<Card> deck = new Deck<>(FixedStoryDeckFactory.buildDeckFor(id));
        Hand<Card> hand = new Hand<>(rules.getHandLimit());
        DiscardPile<Card> discardPile = new DiscardPile<>();
        return new Player<>(id, name, avatar, deck, hand, discardPile);
    }

    public GameState<Card> getState() {
        ensureInitialized();
        return state;
    }

    public ValidationResult moveUnit(String actorPlayerId, String unitId, TilePos destination) {
        ensureInitialized();
        ValidationResult vr = actionValidator.validateMove(state, actorPlayerId, unitId, destination);
        if (!vr.isOk()) return vr;
        moveResolver.resolveMove(state, unitId, destination);
        return ValidationResult.ok();
    }

    public ValidationResult attack(String actorPlayerId, String attackerId, String defenderId) {
        ensureInitialized();
        ValidationResult vr = actionValidator.validateAttack(state, actorPlayerId, attackerId, defenderId);
        if (!vr.isOk()) return vr;
        combatResolver.resolveAttack(state, attackerId, defenderId);
        return ValidationResult.ok();
    }

    public ValidationResult playCardFromHand(String actorPlayerId, int handIndex, CardTarget target) {
        ensureInitialized();
        if (state.isGameOver()) return ValidationResult.fail("game is over");
        if (!state.getActivePlayerId().equals(actorPlayerId)) return ValidationResult.fail("not active player");

        Player<Card> player = state.getPlayer(actorPlayerId);
        if (handIndex < 0 || handIndex >= player.getHand().size()) return ValidationResult.fail("invalid hand index");

        Card card = player.getHand().get(handIndex);
        if (!card.isPlayable(cardPlayContext, actorPlayerId)) {
            return ValidationResult.fail("not enough mana");
        }

        try {
            card.play(cardPlayContext, actorPlayerId, target);
            Card removed = player.getHand().removeAt(handIndex);
            player.getDiscardPile().add(removed);
            return ValidationResult.ok();
        } catch (RuntimeException ex) {
            return ValidationResult.fail(ex.getMessage());
        }
    }

    public ValidationResult endTurn(String actorPlayerId) {
        ensureInitialized();
        if (state.isGameOver()) return ValidationResult.fail("game is over");
        if (!state.getActivePlayerId().equals(actorPlayerId)) return ValidationResult.fail("not active player");

        try {
            turnManager.endTurnAndAdvance(state);
            return ValidationResult.ok();
        } catch (RuntimeException ex) {
            return ValidationResult.fail(ex.getMessage());
        }
    }

    public List<TilePos> getReachableTiles(String unitId) {
        ensureInitialized();
        Unit unit = state.getBoard().getUnitById(unitId).orElse(null);
        if (unit == null) return List.of();
        return actionValidator.getReachableTiles(state.getBoard(), unit);
    }

    public String boardSummary() {
        ensureInitialized();
        StringBuilder sb = new StringBuilder();
        sb.append("Turn=").append(state.getTurnNumber())
          .append(", active=").append(state.getActivePlayerId())
          .append(", status=").append(state.getStatus()).append('\n');
        for (String pid : new String[]{GameState.P1, GameState.P2}) {
            Player<Card> p = state.getPlayer(pid);
            sb.append(pid)
              .append(" mana=").append(p.getMana()).append("/").append(p.getMaxMana())
              .append(" hand=").append(p.getHand().size())
              .append(" deck=").append(p.getDeck().size())
              .append(" discard=").append(p.getDiscardPile().size())
              .append('\n');
        }
        for (Unit u : state.getBoard().getAllUnits()) {
            sb.append("  ").append(u).append('\n');
        }
        return sb.toString();
    }

    private void ensureInitialized() {
        if (state == null) {
            throw new IllegalStateException("GameManager not initialized. Call initializeNewGame() first.");
        }
    }
}
