package events;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import structures.GameState;

/**
 * {
 *   messageType = “cardClicked”
 *   position = <hand index position [1-6]>
 * }
 */
public class CardClicked implements EventProcessor {

	@Override
	public void processEvent(ActorRef out, GameState gameState, JsonNode message) {

		if (gameState == null) return;
		if (!gameState.gameInitalised || gameState.domainGameManager == null || gameState.domainState == null) return;

		// ===== orphan-lock self-heal =====
		// If inputLocked is true but animationGate is NOT locked, we are stuck incorrectly.
		// Recover immediately so the board doesn't feel "dead".
		if (gameState.inputLocked) {
			boolean gateLocked = (gameState.animationGate != null && gameState.animationGate.isLocked());
			if (!gateLocked) {
				gameState.inputLocked = false;
				gameState.animationLockTicks = 0;
				gameState.aiFallbackCooldownTicks = 0;
			} else {
				game.ui.TemplateCommandDispatcher.showNotification(out, "Input locked.");
				return;
			}
		}

		if (!gameState.domainState.getActivePlayerId().equals(game.model.GameState.P1)) return;

		int handPosition = message.get("position").asInt(); // 1..6
		int handIndex = handPosition - 1;
		game.model.Hand<game.card.Card> hand = gameState.domainState.getPlayer(game.model.GameState.P1).getHand();

		// Clicking an empty slot: clear selection so user gets feedback (optional)
		if (handIndex < 0 || handIndex >= hand.size()) {
			gameState.selectedHandPos = null;
			gameState.pendingCompositeHandPos = null;
			gameState.portalStepSourceUnitId = null;

			game.ui.TemplateCommandDispatcher.clearTileHighlights(out, gameState);
			gameState.highlightedTargets.clear();
			game.ui.TemplateCommandDispatcher.showNotification(out, "No card in that hand slot.");
			return;
		}

		// Clicking a card always cancels any "multi-step" targeting in progress.
		gameState.portalStepSourceUnitId = null;
		gameState.pendingCompositeHandPos = null;

		// Toggle selection
		if (gameState.selectedHandPos != null && gameState.selectedHandPos == handPosition) {
			gameState.selectedHandPos = null;
			game.ui.TemplateCommandDispatcher.clearTileHighlights(out, gameState);
			gameState.highlightedTargets.clear();
			return;
		}

		gameState.selectedUnitId = null;
		gameState.selectedHandPos = handPosition;
		game.ui.TemplateCommandDispatcher.clearTileHighlights(out, gameState);
		gameState.highlightedTargets.clear();

		game.card.Card card = hand.get(handIndex);

		// Early mana check: don't keep card selected if we cannot pay (prevents spam)
		int curMana = gameState.domainState.getPlayer(game.model.GameState.P1).getMana();
		int cost = card.getCost();
		if (curMana < cost) {
			gameState.selectedHandPos = null;
			gameState.pendingCompositeHandPos = null;
			gameState.portalStepSourceUnitId = null;

			game.ui.TemplateCommandDispatcher.clearTileHighlights(out, gameState);
			gameState.highlightedTargets.clear();
			game.ui.TemplateCommandDispatcher.showNotification(out, "not enough mana");
			return;
		}

		// Auto-play spells with no target.
		if (card instanceof game.card.SpellCard) {
			game.card.SpellCard sc0 = (game.card.SpellCard) card;

			// Treat null as "no target"
			boolean noTarget = (sc0.getTargetSpec() == null) || sc0.getTargetSpec().isNoTarget();
			if (noTarget) {
				game.system.action.ValidationResult vr0 = gameState.domainGameManager.playCardFromHand(
						game.model.GameState.P1, handIndex, game.card.CardTarget.none());

				if (!vr0.isOk()) {
					game.ui.TemplateCommandDispatcher.showNotification(out, vr0.getMessage());
					return;
				}

				// No reliable UI ack for PLAY_CARD in current template flow.
				// Do not lock input here, otherwise the player waits for heartbeat timeout.
				gameState.inputLocked = false;
				gameState.animationLockTicks = 0;
				gameState.aiFallbackCooldownTicks = 0;

				gameState.selectedHandPos = null;
				gameState.highlightedTargets.clear();
				game.ui.TemplateCommandDispatcher.clearTileHighlights(out, gameState);

				game.ui.TemplateCommandDispatcher.renderAllUnits(out, gameState, gameState.domainState);
				game.ui.TemplateCommandDispatcher.renderPlayerStats(out, gameState.domainState);
				game.ui.TemplateCommandDispatcher.renderHand(out, gameState, gameState.domainState, game.model.GameState.P1);
				return;
			}
		}

		java.util.Set<game.model.TilePos> highlights = new java.util.HashSet<>();

		game.system.action.ActionValidator validator =
				new game.system.action.ActionValidator(new game.system.action.ReachabilityService());

		if (card instanceof game.card.UnitCard) {
			game.card.UnitCard uc = (game.card.UnitCard) card;
			for (int x = 0; x < gameState.domainState.getRules().getBoardWidth(); x++) {
				for (int y = 0; y < gameState.domainState.getRules().getBoardHeight(); y++) {
					game.model.TilePos p = new game.model.TilePos(x, y);
					game.system.action.ValidationResult vr = validator.validateSummon(
							gameState.domainState, game.model.GameState.P1, uc, p);
					if (vr.isOk()) highlights.add(p);
				}
			}
			if (!highlights.isEmpty()) {
				game.ui.TemplateCommandDispatcher.highlightTiles(out, gameState, highlights, 1);
			} else {
				game.ui.TemplateCommandDispatcher.showNotification(out, "No valid summon tiles.");
			}
			return;
		}

		// Step 3: Portal Step two-stage targeting
		if (card instanceof game.card.PortalStepSpellCard) {
			gameState.pendingCompositeHandPos = handPosition;
			gameState.portalStepSourceUnitId = null;

			for (game.model.Unit u : gameState.domainState.getBoard().getAllUnits()) {
				if (!u.getOwnerId().equals(game.model.GameState.P1)) continue;
				if (u instanceof game.model.Avatar) continue;
				highlights.add(u.getPosition());
			}

			game.ui.TemplateCommandDispatcher.clearTileHighlights(out, gameState);
			if (!highlights.isEmpty()) {
				game.ui.TemplateCommandDispatcher.highlightTiles(out, gameState, highlights, 1);
			}
			game.ui.TemplateCommandDispatcher.showNotification(out, "Portal Step: click a friendly unit, then a destination tile.");
			return;
		}

		// Normal spell highlighting: unit targets + tile targets
		if (card instanceof game.card.SpellCard) {
			game.card.SpellCard sc = (game.card.SpellCard) card;

			for (game.model.Unit u : gameState.domainState.getBoard().getAllUnits()) {
				game.system.action.ValidationResult vr = validator.validateSpellTarget(
						gameState.domainState,
						game.model.GameState.P1,
						sc,
						game.card.CardTarget.unit(u.getId())
				);
				if (vr.isOk()) highlights.add(u.getPosition());
			}

			for (int x = 0; x < gameState.domainState.getRules().getBoardWidth(); x++) {
				for (int y = 0; y < gameState.domainState.getRules().getBoardHeight(); y++) {
					game.model.TilePos p = new game.model.TilePos(x, y);
					game.system.action.ValidationResult vr = validator.validateSpellTarget(
							gameState.domainState,
							game.model.GameState.P1,
							sc,
							game.card.CardTarget.tile(p)
					);
					if (vr.isOk()) highlights.add(p);
				}
			}

			if (!highlights.isEmpty()) {
				game.ui.TemplateCommandDispatcher.highlightTiles(out, gameState, highlights, 1);
			} else {
				game.ui.TemplateCommandDispatcher.showNotification(out, "No valid targets.");
			}
		}
	}
}