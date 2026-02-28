package game.ui;

import akka.actor.ActorRef;
import commands.BasicCommands;
import game.model.TilePos;
import game.model.Unit;
import game.model.GameState;
import game.model.Player;
import utils.BasicObjectBuilders;
import utils.StaticConfFiles;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Minimal bridge that renders a subset of the domain game.model.GameState
 * using the template's BasicCommands.
 */
public final class TemplateCommandDispatcher {

	private TemplateCommandDispatcher() {}

	// -------------------------
	// Card config mapping (20 cards provided in conf/gameconfs/cards/)
	// -------------------------
	private static final Map<String, String> CARD_CONF_BY_NAME = new HashMap<>();
	static {
		// NOTE: keys must match domain Card.getName()
		CARD_CONF_BY_NAME.put("Bad Omen", "conf/gameconfs/cards/1_1_c_u_bad_omen.json");
		CARD_CONF_BY_NAME.put("Horn of the Forsaken", "conf/gameconfs/cards/1_2_c_s_hornoftheforsaken.json");
		CARD_CONF_BY_NAME.put("Gloom Chaser", "conf/gameconfs/cards/1_3_c_u_gloom_chaser.json");
		CARD_CONF_BY_NAME.put("Shadow Watcher", "conf/gameconfs/cards/1_4_c_u_shadow_watcher.json");
		CARD_CONF_BY_NAME.put("Wraithling Swarm", "conf/gameconfs/cards/1_5_c_s_wraithling_swarm.json");
		CARD_CONF_BY_NAME.put("Nightsorrow Assassin", "conf/gameconfs/cards/1_6_c_u_nightsorrow_assassin.json");
		CARD_CONF_BY_NAME.put("Rock Pulveriser", "conf/gameconfs/cards/1_7_c_u_rock_pulveriser.json");
		CARD_CONF_BY_NAME.put("Dark Terminus", "conf/gameconfs/cards/1_8_c_s_dark_terminus.json");
		CARD_CONF_BY_NAME.put("Bloodmoon Priestess", "conf/gameconfs/cards/1_9_c_u_bloodmoon_priestess.json");
		CARD_CONF_BY_NAME.put("Shadowdancer", "conf/gameconfs/cards/1_a1_c_u_shadowdancer.json");

		CARD_CONF_BY_NAME.put("Skyrock Golem", "conf/gameconfs/cards/2_1_c_u_skyrock_golem.json");
		CARD_CONF_BY_NAME.put("Swamp Entangler", "conf/gameconfs/cards/2_2_c_u_swamp_entangler.json");
		CARD_CONF_BY_NAME.put("Silverguard Knight", "conf/gameconfs/cards/2_3_c_u_silverguard_knight.json");
		CARD_CONF_BY_NAME.put("Saberspine Tiger", "conf/gameconfs/cards/2_4_c_u_saberspine_tiger.json");
		CARD_CONF_BY_NAME.put("Beam Shock", "conf/gameconfs/cards/2_5_c_s_beamshock.json");
		CARD_CONF_BY_NAME.put("Young Flamewing", "conf/gameconfs/cards/2_6_c_u_young_flamewing.json");
		CARD_CONF_BY_NAME.put("Silverguard Squire", "conf/gameconfs/cards/2_7_c_u_silverguard_squire.json");
		CARD_CONF_BY_NAME.put("Ironcliff Guardian", "conf/gameconfs/cards/2_8_c_u_ironcliff_guardian.json");
		CARD_CONF_BY_NAME.put("Sundrop Elixir", "conf/gameconfs/cards/2_9_c_s_sundrop_elixir.json");
		CARD_CONF_BY_NAME.put("Truestrike", "conf/gameconfs/cards/2_a1_c_s_truestrike.json");
	}

	public static void renderInitialBoardAndAvatars(ActorRef out, game.model.GameState<?> domainState) {
		for (int x = 0; x < domainState.getRules().getBoardWidth(); x++) {
			for (int y = 0; y < domainState.getRules().getBoardHeight(); y++) {
				BasicCommands.drawTile(out, BasicObjectBuilders.loadTile(x, y), 0);
			}
		}

		drawAvatar(out, domainState.getPlayer(game.model.GameState.P1).getAvatar(), StaticConfFiles.humanAvatar);
		drawAvatar(out, domainState.getPlayer(game.model.GameState.P2).getAvatar(), StaticConfFiles.aiAvatar);
	}

	private static void drawAvatar(ActorRef out, Unit avatar, String avatarConf) {
		TilePos p = avatar.getPosition();
		structures.basic.Tile tile = BasicObjectBuilders.loadTile(p.x(), p.y());

		structures.basic.Unit visual = BasicObjectBuilders.loadUnit(avatarConf, avatar.getId().hashCode(), structures.basic.Unit.class);
		BasicCommands.drawUnit(out, visual, tile);
		BasicCommands.setUnitHealth(out, visual, avatar.getHp());
		BasicCommands.setUnitAttack(out, visual, avatar.getAttack()); // FIX: getAttack()
	}

	// ---------------------------------------------------------------------
	// Units, stats, highlighting
	// ---------------------------------------------------------------------

	public static void renderPlayerStats(ActorRef out, GameState<?> domainState) {
		Player<?> p1 = domainState.getPlayer(GameState.P1);
		Player<?> p2 = domainState.getPlayer(GameState.P2);

		structures.basic.Player bp1 = new structures.basic.Player(p1.getAvatar().getHp(), p1.getMana());
		structures.basic.Player bp2 = new structures.basic.Player(p2.getAvatar().getHp(), p2.getMana());
		BasicCommands.setPlayer1Health(out, bp1);
		BasicCommands.setPlayer2Health(out, bp2);
		BasicCommands.setPlayer1Mana(out, bp1);
		BasicCommands.setPlayer2Mana(out, bp2);
	}

	public static structures.basic.Unit ensureVisualUnit(
			ActorRef out,
			structures.GameState templateState,
			Unit domainUnit
	) {
		structures.basic.Unit cached = templateState.visualUnits.get(domainUnit.getId());
		if (cached != null) return cached;

		String conf = pickUnitConf(domainUnit);
		structures.basic.Unit visual = BasicObjectBuilders.loadUnit(conf, domainUnit.getId().hashCode(), structures.basic.Unit.class);
		templateState.visualUnits.put(domainUnit.getId(), visual);
		return visual;
	}

	public static void renderAllUnits(ActorRef out, structures.GameState templateState, GameState<?> domainState) {
		for (Unit u : domainState.getBoard().getAllUnits()) {
			structures.basic.Unit visual = ensureVisualUnit(out, templateState, u);
			structures.basic.Tile tile = BasicObjectBuilders.loadTile(u.getPosition().x(), u.getPosition().y());
			BasicCommands.drawUnit(out, visual, tile);
			BasicCommands.setUnitHealth(out, visual, u.getHp());
			BasicCommands.setUnitAttack(out, visual, u.getAttack()); // FIX: getAttack()
		}
	}

	public static void moveUnit(ActorRef out, structures.GameState templateState, Unit domainUnit) {
		structures.basic.Unit visual = ensureVisualUnit(out, templateState, domainUnit);
		structures.basic.Tile tile = BasicObjectBuilders.loadTile(domainUnit.getPosition().x(), domainUnit.getPosition().y());
		BasicCommands.moveUnitToTile(out, visual, tile);
	}

	public static void deleteUnitIfPresent(ActorRef out, structures.GameState templateState, String unitId) {
		structures.basic.Unit visual = templateState.visualUnits.remove(unitId);
		if (visual != null) {
			BasicCommands.deleteUnit(out, visual);
		}
	}

	public static void highlightTiles(ActorRef out, structures.GameState templateState, Set<TilePos> tiles, int mode) {
		clearTileHighlights(out, templateState);
		for (TilePos p : tiles) {
			templateState.highlightedTiles.add(p.x() + "," + p.y());
			BasicCommands.drawTile(out, BasicObjectBuilders.loadTile(p.x(), p.y()), mode);
		}
	}

	public static void clearTileHighlights(ActorRef out, structures.GameState templateState) {
		if (templateState.highlightedTiles.isEmpty()) return;
		for (String key : new HashSet<>(templateState.highlightedTiles)) {
			templateState.highlightedTiles.remove(key);
			String[] parts = key.split(",");
			int x = Integer.parseInt(parts[0]);
			int y = Integer.parseInt(parts[1]);
			BasicCommands.drawTile(out, BasicObjectBuilders.loadTile(x, y), 0);
		}
	}

	public static void showNotification(ActorRef out, String text) {
		BasicCommands.addPlayer1Notification(out, text, 3);
	}

	private static String pickUnitConf(Unit domainUnit) {
		String name = domainUnit.getName();
		String file = name.toLowerCase().replace(" ", "_")
				.replace("-", "_")
				.replace("'", "")
				.replace("/", "_");

		if ("wraithling".equals(file)) {
			return StaticConfFiles.wraithling;
		}
		return "conf/gameconfs/units/" + file + ".json";
	}

	// ---------------------------------------------------------------------
	// Step2: Hand rendering (positions 1..6)
	// ---------------------------------------------------------------------

	public static void renderHand(ActorRef out, structures.GameState templateState, game.model.GameState<game.card.Card> domainState, String playerId) {
		if (domainState == null) return;

		java.util.List<game.card.Card> hand = domainState.getPlayer(playerId).getHand().snapshot();

		for (int pos = 1; pos <= 6; pos++) {
			int idx = pos - 1;

			if (idx < hand.size()) {
				game.card.Card c = hand.get(idx);
				String conf = CARD_CONF_BY_NAME.getOrDefault(c.getName(), "conf/gameconfs/cards/1_1_c_u_bad_omen.json");

				structures.basic.Card visualCard = BasicObjectBuilders.loadCard(conf, c.getId().hashCode(), structures.basic.Card.class);
				templateState.visualHand.put(pos, visualCard);

				BasicCommands.drawCard(out, visualCard, pos, 0);
			} else {
				templateState.visualHand.remove(pos);
				BasicCommands.deleteCard(out, pos);
			}
		}
	}
}