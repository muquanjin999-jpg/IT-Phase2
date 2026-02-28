package structures;

/**
 * This class can be used to hold information about the on-going game.
 * Its created with the GameActor.
 *
 * @author Dr. Richard McCreadie
 */
public class GameState {

	public boolean gameInitalised = false;

	public boolean something = false;

	// -----------------------------
	// Phase 2 UI interaction state
	// -----------------------------
	/**
	 * When true, ignore gameplay input (used as a lightweight async gate).
	 *
	 * We lock input during animations/actions and unlock via AnimationEnded(tag)
	 * (or UnitStopped for movement).
	 */
	public boolean inputLocked = false;

	/** The domain unit currently selected by the human player (nullable). */
	public String selectedUnitId = null;
	/** The human hand position (1-6) currently selected (nullable). */
	public Integer selectedHandPos = null;

	/**
	 * A minimal view-model cache that maps domain unit ids -> rendered template Unit objects.
	 * Required so we can later move/delete/update the *same* unit instance in the UI.
	 */
	public java.util.Map<String, structures.basic.Unit> visualUnits = new java.util.HashMap<>();

	/** Hand position (1-6) -> rendered template Card instance. */
	public java.util.Map<Integer, structures.basic.Card> visualHand = new java.util.HashMap<>();

	/** Currently highlighted tiles on the UI (as "x,y" keys). */
	public java.util.Set<String> highlightedTiles = new java.util.HashSet<>();
	/** Currently highlighted enemy unit ids (domain ids). */
	public java.util.Set<String> highlightedTargets = new java.util.HashSet<>();

	/**
	 * Domain-layer game manager (your Phase 2 backend). Stored here so all EventProcessor
	 * classes can access the same match instance.
	 */
	public game.core.GameManager domainGameManager = null;

	/**
	 * Domain-layer match state created by {@link game.core.GameManager#initializeNewGame()}.
	 */
	public game.model.GameState<game.card.Card> domainState = null;

	// -----------------------------
	// Phase 2 AI loop state (Step1)
	// -----------------------------
	public game.ai.AIController aiController = null;
	public boolean aiTurnActive = false;
	public int aiActionsThisTurn = 0;

	// -----------------------------
	// Animation gating (Step1+gating)
	// -----------------------------
	public game.ui.AnimationGate animationGate = new game.ui.AnimationGate();

	/**
	 * Fallback cooldown ticks (only used if UI does not send AnimationEnded(tag)).
	 * If you DO implement proper UI ack, you can keep this at 0 always.
	 */
	public int aiFallbackCooldownTicks = 0;

}