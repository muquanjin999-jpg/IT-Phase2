package structures;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class can be used to hold information about the on-going game.
 * It's created with the GameActor.
 *
 * Note: This is a UI-layer state holder (Template side). The real game state lives in
 * {@link game.model.GameState} under gameState.domainState.
 */
public class GameState {
	
	// ===== UI init sequencing =====
	// During initial render, frontend queues drawUnit but labels aren't created until the render loop.
	// So we must delay setUnitHealth/setUnitAttack until at least the next heartbeat.
    public boolean uiInitialUnitsDrawn = false;
    public Map<String, Integer> pendingUnitHp = new HashMap<>();
    public Map<String, Integer> pendingUnitAtk = new HashMap<>();
    public boolean initialRenderPendingUnlock = false;

    public boolean gameInitalised = false;

    // -----------------------------
    // Phase 2 UI interaction state
    // -----------------------------
    public boolean inputLocked = false;

    /**
     * Counts Heartbeat ticks while animationGate is locked.
     * Used as a server-side safety net when the UI does not emit AnimationEnded(tag).
     */
    public int animationLockTicks = 0;

    /**
     * Max Heartbeat ticks to wait before force-unlocking.
     * Heartbeat fires roughly once per second, so 3 means ~3 seconds.
     */
    public static final int ANIMATION_LOCK_TIMEOUT_TICKS = 3;

    /** Selected unit id (as String in template layer) */
    public String selectedUnitId = null;
    /** Selected hand position [1..6] */
    public Integer selectedHandPos = null;

    // -----------------------------
    // Step 3: Portal Step two-stage targeting
    // -----------------------------
    /** First-stage chosen friendly unit id for Portal Step */
    public String portalStepSourceUnitId = null;

    /** If the selected card requires composite targeting, store its hand position */
    public Integer pendingCompositeHandPos = null;

    // -----------------------------
    // Template visual caches (helps re-render / highlight)
    // -----------------------------
    public Map<String, structures.basic.Unit> visualUnits = new HashMap<>();
    public Map<Integer, structures.basic.Card> visualHand = new HashMap<>();

    /** highlighted tile keys like "x,y" */
    public Set<String> highlightedTiles = new HashSet<>();

    /** highlighted target unit ids */
    public Set<String> highlightedTargets = new HashSet<>();

    // -----------------------------
    // Domain layer bindings (your actual Phase2 implementation)
    // -----------------------------
    public game.core.GameManager domainGameManager = null;
    public game.model.GameState<game.card.Card> domainState = null;

    // -----------------------------
    // Async gating (AnimationEnded(tag) ack)
    // -----------------------------
    public game.ui.AnimationGate animationGate = new game.ui.AnimationGate();

    // -----------------------------
    // AI loop state (driven by Heartbeat)
    // -----------------------------
    public game.ai.AIController aiController = null;

    /** When true, Heartbeat will step AI actions (only if activePlayer == P2) */
    public boolean aiTurnActive = false;

    /** Per-turn cap counter */
    public int aiActionsThisTurn = 0;

    /**
     * Fallback cooldown ticks (only used if UI never sends AnimationEnded).
     * If you later implement proper AnimationEnded tags from UI, this can stay but should be 0 always.
     */
    public int aiFallbackCooldownTicks = 0;
}