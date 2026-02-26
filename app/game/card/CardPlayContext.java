package game.card;

import game.model.GameState;
import game.system.action.ActionValidator;
import game.system.resolve.DeathResolver;
import game.system.turn.ManaSystem;

public class CardPlayContext {
    private final GameState<Card> state;
    private final ManaSystem manaSystem;
    private final ActionValidator actionValidator;
    private final DeathResolver deathResolver;
    private final IdGenerator idGenerator;

    public CardPlayContext(
            GameState<Card> state,
            ManaSystem manaSystem,
            ActionValidator actionValidator,
            DeathResolver deathResolver,
            IdGenerator idGenerator
    ) {
        this.state = state;
        this.manaSystem = manaSystem;
        this.actionValidator = actionValidator;
        this.deathResolver = deathResolver;
        this.idGenerator = idGenerator;
    }

    public GameState<Card> getState() { return state; }
    public ManaSystem getManaSystem() { return manaSystem; }
    public ActionValidator getActionValidator() { return actionValidator; }
    public DeathResolver getDeathResolver() { return deathResolver; }
    public IdGenerator getIdGenerator() { return idGenerator; }
}
