package structures;

/**
 * This class can be used to hold information about the on-going game.
 * Its created with the GameActor.
 * 
 * @author Dr. Richard McCreadie
 *
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
	 * The provided template does not include explicit AnimationEnded(tag) events.
	 * We therefore lock input for move animations using unitMoving/unitStopped.
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
	 * classes (Initalize/TileClicked/CardClicked/EndTurnClicked/UnitMoving/UnitStopped)
	 * can access the same match instance.
	 */
	public game.core.GameManager domainGameManager = null;

	/**
	 * Domain-layer match state created by {@link game.core.GameManager#initializeNewGame()}.
	 */
	public game.model.GameState<game.card.Card> domainState = null;
	
	// -----------------------------
	// Phase 2 AI turn-loop state
	// -----------------------------
	/** AI controller (runs only when active player is P2). */
	public game.ai.AIController aiController = null;

	/** True when we are currently executing the AI turn via heartbeat stepping. */
	public boolean aiTurnActive = false;

	/** AI per-turn action cap to prevent loops (recommend 8). */
	public int aiActionsThisTurn = 0;

	/**
	 * Simple cooldown ticks so UI has time to render between non-move actions
	 * (since template doesn't provide AnimationEnded(tag) for attack/spell).
	 */
	public int aiCooldownTicks = 0;
	
}
