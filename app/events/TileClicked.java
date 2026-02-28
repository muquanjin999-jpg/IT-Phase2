package events;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import utils.BasicObjectBuilders;

/**
 * Tile clicked event.
 */
public class TileClicked implements EventProcessor {

	@Override
	public void processEvent(ActorRef out, GameState gameState, JsonNode message) {
		if (!gameState.gameInitalised || gameState.domainGameManager == null || gameState.domainState == null) return;
		if (gameState.inputLocked) return;

		int tilex = message.get("tilex").asInt() - 1;
		int tiley = message.get("tiley").asInt() - 1;
		game.model.TilePos clicked = new game.model.TilePos(tilex, tiley);

		// If a hand card is selected, treat click as a card target
		if (gameState.selectedHandPos != null) {
			handleCardTargetClick(out, gameState, clicked);
			return;
		}

		String key = tilex + "," + tiley;

		// Move
		if (gameState.selectedUnitId != null && gameState.highlightedTiles.contains(key)) {
			game.system.action.ValidationResult vr = gameState.domainGameManager.moveUnit(game.model.GameState.P1, gameState.selectedUnitId, clicked);
			if (!vr.isOk()) {
				game.ui.TemplateCommandDispatcher.showNotification(out, vr.getMessage());
				return;
			}
			game.ui.TemplateCommandDispatcher.clearTileHighlights(out, gameState);
			gameState.highlightedTargets.clear();

			game.model.Unit u = gameState.domainState.getBoard().getUnitById(gameState.selectedUnitId).orElse(null);
			if (u != null) {
				game.ui.TemplateCommandDispatcher.moveUnit(out, gameState, u);
			}
			gameState.inputLocked = true; // unlocked by UnitStopped
			return;
		}

		// Determine what is on the clicked tile
		game.model.Board board = gameState.domainState.getBoard();
		java.util.Optional<game.model.Unit> opt = board.getUnitAt(clicked);
		if (opt.isEmpty()) {
			gameState.selectedUnitId = null;
			game.ui.TemplateCommandDispatcher.clearTileHighlights(out, gameState);
			gameState.highlightedTargets.clear();
			return;
		}

		game.model.Unit clickedUnit = opt.get();
		boolean isFriendly = clickedUnit.getOwnerId().equals(game.model.GameState.P1);

		// Attack
		if (gameState.selectedUnitId != null && !isFriendly && gameState.highlightedTargets.contains(clickedUnit.getId())) {
			game.system.action.ValidationResult vr = gameState.domainGameManager.attack(game.model.GameState.P1, gameState.selectedUnitId, clickedUnit.getId());
			if (!vr.isOk()) {
				game.ui.TemplateCommandDispatcher.showNotification(out, vr.getMessage());
				return;
			}
			syncBoardAfterCombat(out, gameState);
			game.ui.TemplateCommandDispatcher.clearTileHighlights(out, gameState);
			gameState.highlightedTargets.clear();
			return;
		}

		// Select friendly unit
		if (isFriendly && gameState.domainState.getActivePlayerId().equals(game.model.GameState.P1)) {
			gameState.selectedUnitId = clickedUnit.getId();
			highlightForSelectedUnit(out, gameState, clickedUnit);
		}
	}

	private void handleCardTargetClick(ActorRef out, GameState gameState, game.model.TilePos clicked) {
		int handPos = gameState.selectedHandPos;
		int handIndex = handPos - 1;

		game.model.Unit unit = gameState.domainState.getBoard().getUnitAt(clicked).orElse(null);
		game.card.CardTarget target = (unit != null) ? game.card.CardTarget.unit(unit.getId()) : game.card.CardTarget.tile(clicked);

		game.system.action.ValidationResult vr = gameState.domainGameManager.playCardFromHand(game.model.GameState.P1, handIndex, target);
		if (!vr.isOk()) {
			game.ui.TemplateCommandDispatcher.showNotification(out, vr.getMessage());
			return;
		}

		gameState.selectedHandPos = null;
		game.ui.TemplateCommandDispatcher.clearTileHighlights(out, gameState);
		gameState.highlightedTargets.clear();

		// Re-render board + stats + Step2 hand
		game.ui.TemplateCommandDispatcher.renderAllUnits(out, gameState, gameState.domainState);
		game.ui.TemplateCommandDispatcher.renderPlayerStats(out, gameState.domainState);
		game.ui.TemplateCommandDispatcher.renderHand(out, gameState, gameState.domainState, game.model.GameState.P1);
	}

	private void highlightForSelectedUnit(ActorRef out, GameState gameState, game.model.Unit unit) {
		java.util.Set<game.model.TilePos> tiles = new java.util.HashSet<>(gameState.domainGameManager.getReachableTiles(unit.getId()));
		game.ui.TemplateCommandDispatcher.highlightTiles(out, gameState, tiles, 1);

		gameState.highlightedTargets.clear();
		game.model.Board board = gameState.domainState.getBoard();

		boolean enemyHasProtector = board.getUnitsByOwner(game.model.GameState.P2).stream()
				.anyMatch(u -> u.hasKeyword(game.model.UnitKeyword.PROTECTOR) && u.getHp() > 0);

		game.system.action.ActionValidator validator = new game.system.action.ActionValidator(new game.system.action.ReachabilityService());

		for (game.model.Unit enemy : board.getUnitsByOwner(game.model.GameState.P2)) {
			if (enemy.getHp() <= 0) continue;

			// FIX: avatar detection by instanceof Avatar
			boolean enemyIsAvatar = (enemy instanceof game.model.Avatar);
			if (enemyIsAvatar && enemyHasProtector) continue;

			game.system.action.ValidationResult v =
					validator.validateAttack(gameState.domainState, game.model.GameState.P1, unit.getId(), enemy.getId());

			if (v.isOk()) {
				gameState.highlightedTargets.add(enemy.getId());
				gameState.highlightedTiles.add(enemy.getPosition().x() + "," + enemy.getPosition().y());
				BasicCommands.drawTile(out, BasicObjectBuilders.loadTile(enemy.getPosition().x(), enemy.getPosition().y()), 2);
			}
		}
	}

	private void syncBoardAfterCombat(ActorRef out, GameState gameState) {
		java.util.Set<String> alive = new java.util.HashSet<>();
		for (game.model.Unit u : gameState.domainState.getBoard().getAllUnits()) {
			if (u.getHp() > 0) alive.add(u.getId());
		}
		for (String knownId : new java.util.HashSet<>(gameState.visualUnits.keySet())) {
			if (!alive.contains(knownId)) {
				game.ui.TemplateCommandDispatcher.deleteUnitIfPresent(out, gameState, knownId);
			}
		}
		game.ui.TemplateCommandDispatcher.renderAllUnits(out, gameState, gameState.domainState);
		game.ui.TemplateCommandDispatcher.renderPlayerStats(out, gameState.domainState);
		game.ui.TemplateCommandDispatcher.renderHand(out, gameState, gameState.domainState, game.model.GameState.P1);

		if (gameState.domainState.isGameOver()) {
			String winner = gameState.domainState.getWinnerPlayerId().orElse("?");
			game.ui.TemplateCommandDispatcher.showNotification(out, "Game Over. Winner: " + winner);
		}
	}
}