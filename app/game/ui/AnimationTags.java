package game.ui;

/**
 * Unified tags for AnimationGate.lock/unlock and UI AnimationEnded(tag).
 * Keep these stable to avoid tag mismatches.
 */
public final class AnimationTags {
	private AnimationTags() {}

	public static final String INITIAL_RENDER = "initial_render";
	public static final String MOVE = "move";
	public static final String ATTACK = "attack";
	public static final String PLAY_CARD = "play_card";
	public static final String END_TURN = "end_turn";
}