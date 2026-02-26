package events;


import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import structures.GameState;

/**
 * Indicates that the user has clicked an object on the game canvas, in this case a card.
 * The event returns the position in the player's hand the card resides within.
 * 
 * { 
 *   messageType = “cardClicked”
 *   position = <hand index position [1-6]>
 * }
 * 
 * @author Dr. Richard McCreadie
 *
 */
public class CardClicked implements EventProcessor{

	@Override
	public void processEvent(ActorRef out, GameState gameState, JsonNode message) {
		if (!gameState.gameInitalised || gameState.domainGameManager == null || gameState.domainState == null) return;
		if (gameState.inputLocked) return;
		if (!gameState.domainState.getActivePlayerId().equals(game.model.GameState.P1)) return;

		int handPosition = message.get("position").asInt(); // 1..6
		int handIndex = handPosition - 1;
		game.model.Hand<game.card.Card> hand = gameState.domainState.getPlayer(game.model.GameState.P1).getHand();
		if (handIndex < 0 || handIndex >= hand.size()) return;

		// Toggle selection
		if (gameState.selectedHandPos != null && gameState.selectedHandPos == handPosition) {
			gameState.selectedHandPos = null;
			game.ui.TemplateCommandDispatcher.clearTileHighlights(out, gameState);
			return;
		}
		gameState.selectedUnitId = null;
		gameState.selectedHandPos = handPosition;
		game.ui.TemplateCommandDispatcher.clearTileHighlights(out, gameState);
		gameState.highlightedTargets.clear();

		game.card.Card card = hand.get(handIndex);
		java.util.Set<game.model.TilePos> targets = new java.util.HashSet<game.model.TilePos>();

		// We cannot call playCardFromHand for validation (it mutates). Use ActionValidator directly.
		game.system.action.ActionValidator validator = new game.system.action.ActionValidator(new game.system.action.ReachabilityService());
		if (card instanceof game.card.UnitCard) {
			game.card.UnitCard uc = (game.card.UnitCard) card;
			for (int x = 0; x < gameState.domainState.getRules().getBoardWidth(); x++) {
				for (int y = 0; y < gameState.domainState.getRules().getBoardHeight(); y++) {
					game.model.TilePos p = new game.model.TilePos(x, y);
					game.system.action.ValidationResult vr = validator.validateSummon(gameState.domainState, game.model.GameState.P1, uc, p);
					if (vr.isOk()) targets.add(p);
				}
			}
		} else if (card instanceof game.card.PortalStepSpellCard) {
			// Portal Step uses unit+tile targeting; we simplify by highlighting tiles only.
			// Player must first select a friendly unit, then a tile.
			game.ui.TemplateCommandDispatcher.showNotification(out, "Portal Step: click a friendly unit, then a tile.");
		} else if (card instanceof game.card.SpellCard) {
			game.card.SpellCard sc = (game.card.SpellCard) card;
			// 1) Unit targets (highlight as mode=2)
			for (game.model.Unit u : gameState.domainState.getBoard().getAllUnits()) {
				game.system.action.ValidationResult vr = validator.validateSpellTarget(gameState.domainState, game.model.GameState.P1, sc, game.card.CardTarget.unit(u.getId()));
				if (vr.isOk()) {
					targets.add(u.getPosition());
				}
			}
			// 2) Tile targets
			for (int x = 0; x < gameState.domainState.getRules().getBoardWidth(); x++) {
				for (int y = 0; y < gameState.domainState.getRules().getBoardHeight(); y++) {
					game.model.TilePos p = new game.model.TilePos(x, y);
					game.system.action.ValidationResult vr = validator.validateSpellTarget(gameState.domainState, game.model.GameState.P1, sc, game.card.CardTarget.tile(p));
					if (vr.isOk()) targets.add(p);
				}
			}
		}

		if (!targets.isEmpty()) {
			game.ui.TemplateCommandDispatcher.highlightTiles(out, gameState, targets, 1);
		}
		
	}

}
