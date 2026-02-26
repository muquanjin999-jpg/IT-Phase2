package events;


import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import utils.BasicObjectBuilders;

/**
 * Indicates that the user has clicked an object on the game canvas, in this case a tile.
 * The event returns the x (horizontal) and y (vertical) indices of the tile that was
 * clicked. Tile indices start at 1.
 * 
 * { 
 *   messageType = “tileClicked”
 *   tilex = <x index of the tile>
 *   tiley = <y index of the tile>
 * }
 * 
 * @author Dr. Richard McCreadie
 *
 */
public class TileClicked implements EventProcessor{

	@Override
	public void processEvent(ActorRef out, GameState gameState, JsonNode message) {
		if (!gameState.gameInitalised || gameState.domainGameManager == null || gameState.domainState == null) return;
		if (gameState.inputLocked) return;

		// Template indices start at 1. Our domain uses 0-based (0..8, 0..4).
		int tilex = message.get("tilex").asInt() - 1;
		int tiley = message.get("tiley").asInt() - 1;
		game.model.TilePos clicked = new game.model.TilePos(tilex, tiley);

		// 0) If a hand card is selected, treat click as a card target (summon/spell)
		if (gameState.selectedHandPos != null) {
			handleCardTargetClick(out, gameState, clicked);
			return;
		}

		// 1) If a unit is selected and the clicked tile is highlighted as a move destination => move.
		String key = tilex + "," + tiley;
		if (gameState.selectedUnitId != null && gameState.highlightedTiles.contains(key)) {
			game.system.action.ValidationResult vr = gameState.domainGameManager.moveUnit(game.model.GameState.P1, gameState.selectedUnitId, clicked);
			if (!vr.isOk()) {
				game.ui.TemplateCommandDispatcher.showNotification(out, vr.getMessage());
				return;
			}
			// Re-render: move visual unit + clear highlights
			game.ui.TemplateCommandDispatcher.clearTileHighlights(out, gameState);
			gameState.highlightedTargets.clear();
			game.model.Unit u = gameState.domainState.getBoard().getUnitById(gameState.selectedUnitId).orElse(null);
			if (u != null) {
				game.ui.TemplateCommandDispatcher.moveUnit(out, gameState, u);
			}
			gameState.inputLocked = true; // unlocked by UnitStopped
			return;
		}

		// 2) Determine what is on the clicked tile.
		game.model.Board board = gameState.domainState.getBoard();
		java.util.Optional<game.model.Unit> opt = board.getUnitAt(clicked);
		if (opt.isEmpty()) {
			// Clicked empty tile: clear selection + highlights
			gameState.selectedUnitId = null;
			game.ui.TemplateCommandDispatcher.clearTileHighlights(out, gameState);
			gameState.highlightedTargets.clear();
			return;
		}

		game.model.Unit clickedUnit = opt.get();
		boolean isFriendly = clickedUnit.getOwnerId().equals(game.model.GameState.P1);
		
		// 3) If a unit is selected and clicked an enemy unit that is a highlighted target => attack.
		if (gameState.selectedUnitId != null && !isFriendly && gameState.highlightedTargets.contains(clickedUnit.getId())) {
			game.system.action.ValidationResult vr = gameState.domainGameManager.attack(game.model.GameState.P1, gameState.selectedUnitId, clickedUnit.getId());
			if (!vr.isOk()) {
				game.ui.TemplateCommandDispatcher.showNotification(out, vr.getMessage());
				return;
			}
			// Sync visuals: update HP/deaths for any affected units
			syncBoardAfterCombat(out, gameState);
			game.ui.TemplateCommandDispatcher.clearTileHighlights(out, gameState);
			gameState.highlightedTargets.clear();
			return;
		}

		// 4) Otherwise, selecting a friendly unit highlights possible moves (and attack targets).
		if (isFriendly && gameState.domainState.getActivePlayerId().equals(game.model.GameState.P1)) {
			gameState.selectedUnitId = clickedUnit.getId();
			highlightForSelectedUnit(out, gameState, clickedUnit);
		}
		
	}

	private void handleCardTargetClick(ActorRef out, GameState gameState, game.model.TilePos clicked) {
		int handPos = gameState.selectedHandPos;
		int handIndex = handPos - 1; // domain hand is 0-based
		// If a unit exists on the clicked tile, allow unit-targeting spells.
		game.model.Unit unit = gameState.domainState.getBoard().getUnitAt(clicked).orElse(null);
		game.card.CardTarget target = (unit != null) ? game.card.CardTarget.unit(unit.getId()) : game.card.CardTarget.tile(clicked);
		game.system.action.ValidationResult vr = gameState.domainGameManager.playCardFromHand(game.model.GameState.P1, handIndex, target);
		if (!vr.isOk()) {
			game.ui.TemplateCommandDispatcher.showNotification(out, vr.getMessage());
			return;
		}
		// Clear selection & highlights after a play
		gameState.selectedHandPos = null;
		game.ui.TemplateCommandDispatcher.clearTileHighlights(out, gameState);
		gameState.highlightedTargets.clear();
		// Re-render board units and stats (simple approach)
		game.ui.TemplateCommandDispatcher.renderAllUnits(out, gameState, gameState.domainState);
		game.ui.TemplateCommandDispatcher.renderPlayerStats(out, gameState.domainState);
	}

	private void highlightForSelectedUnit(ActorRef out, GameState gameState, game.model.Unit unit) {
		// Movement highlights
		java.util.Set<game.model.TilePos> tiles = new java.util.HashSet<game.model.TilePos>(gameState.domainGameManager.getReachableTiles(unit.getId()));
		game.ui.TemplateCommandDispatcher.highlightTiles(out, gameState, tiles, 1);

		// Attack target highlights: enemy units in range, plus avatar if no protector
		gameState.highlightedTargets.clear();
		game.model.Board board = gameState.domainState.getBoard();
		boolean enemyHasProtector = board.getUnitsByOwner(game.model.GameState.P2).stream()
				.anyMatch(u -> u.hasKeyword(game.model.UnitKeyword.PROTECTOR) && u.getHp() > 0);
		// IMPORTANT: we must not mutate state in highlight. Use validator directly.
		for (game.model.Unit enemy : board.getUnitsByOwner(game.model.GameState.P2)) {
			if (enemy.getHp() <= 0) continue;
			if (enemy.isAvatar() && enemyHasProtector) continue;
			game.system.action.ValidationResult v = new game.system.action.ActionValidator(new game.system.action.ReachabilityService())
					.validateAttack(gameState.domainState, game.model.GameState.P1, unit.getId(), enemy.getId());
			if (v.isOk()) {
				gameState.highlightedTargets.add(enemy.getId());
				gameState.highlightedTiles.add(enemy.getPosition().x() + "," + enemy.getPosition().y());
				BasicCommands.drawTile(out, BasicObjectBuilders.loadTile(enemy.getPosition().x(), enemy.getPosition().y()), 2);
			}
		}
	}

	private void syncBoardAfterCombat(ActorRef out, GameState gameState) {
		// Remove dead units visually
		java.util.Set<String> alive = new java.util.HashSet<>();
		for (game.model.Unit u : gameState.domainState.getBoard().getAllUnits()) {
			if (u.getHp() > 0) alive.add(u.getId());
		}
		for (String knownId : new java.util.HashSet<>(gameState.visualUnits.keySet())) {
			if (!alive.contains(knownId)) {
				game.ui.TemplateCommandDispatcher.deleteUnitIfPresent(out, gameState, knownId);
			}
		}
		// Update HP/ATK of remaining units
		game.ui.TemplateCommandDispatcher.renderAllUnits(out, gameState, gameState.domainState);
		game.ui.TemplateCommandDispatcher.renderPlayerStats(out, gameState.domainState);
		if (gameState.domainState.isGameOver()) {
			String winner = gameState.domainState.getWinnerPlayerId().orElse("?");
			game.ui.TemplateCommandDispatcher.showNotification(out, "Game Over. Winner: " + winner);
		}
	}

}
