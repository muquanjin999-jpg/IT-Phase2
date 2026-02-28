package game.ui;

import java.util.HashSet;
import java.util.Set;

/**
 * A minimal animation/action gating utility.
 * Use lock(tag) before issuing an animation/action, and unlock(tag) when the UI acks completion.
 */
public class AnimationGate {

	private final Set<String> locks = new HashSet<>();

	public void lock(String tag) {
		if (tag == null) return;
		locks.add(tag);
	}

	public void unlock(String tag) {
		if (tag == null) return;
		locks.remove(tag);
	}

	public boolean isLocked() {
		return !locks.isEmpty();
	}

	public int getLockCount() {
		return locks.size();
	}
}