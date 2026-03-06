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
        CARD_CONF_BY_NAME.put("Ironcliffe Guardian", "conf/gameconfs/cards/2_8_c_u_ironcliff_guardian.json");
        CARD_CONF_BY_NAME.put("Sundrop Elixir", "conf/gameconfs/cards/2_9_c_s_sundrop_elixir.json");
        CARD_CONF_BY_NAME.put("Truestrike", "conf/gameconfs/cards/2_a1_c_s_truestrike.json");
    }

    // CHANGED: accept templateState so we can cache avatars + delay stats
    public static void renderInitialBoardAndAvatars(ActorRef out, structures.GameState templateState, game.model.GameState<?> domainState) {
        for (int x = 0; x < domainState.getRules().getBoardWidth(); x++) {
            for (int y = 0; y < domainState.getRules().getBoardHeight(); y++) {
                BasicCommands.drawTile(out, BasicObjectBuilders.loadTile(x, y), 0);
            }
        }

        drawAvatar(out, templateState, domainState.getPlayer(game.model.GameState.P1).getAvatar(), StaticConfFiles.humanAvatar);
        drawAvatar(out, templateState, domainState.getPlayer(game.model.GameState.P2).getAvatar(), StaticConfFiles.aiAvatar);
    }

    // CHANGED: cache visual + delay health/attack until UI has created text objects
    private static void drawAvatar(ActorRef out, structures.GameState templateState, Unit avatar, String avatarConf) {
        TilePos p = avatar.getPosition();
        structures.basic.Tile tile = BasicObjectBuilders.loadTile(p.x(), p.y());

        structures.basic.Unit visual = BasicObjectBuilders.loadUnit(avatarConf, avatar.getId().hashCode(), structures.basic.Unit.class);
        templateState.visualUnits.put(avatar.getId(), visual);

        BasicCommands.drawUnit(out, visual, tile);

        if (!templateState.uiInitialUnitsDrawn) {
            templateState.pendingUnitHp.put(avatar.getId(), avatar.getHp());
            templateState.pendingUnitAtk.put(avatar.getId(), avatar.getAttack());
        } else {
            BasicCommands.setUnitHealth(out, visual, avatar.getHp());
            BasicCommands.setUnitAttack(out, visual, avatar.getAttack());
        }
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
        // 1) collect current domain unit ids
        Set<String> currentIds = new HashSet<>();
        for (Unit u : domainState.getBoard().getAllUnits()) {
            currentIds.add(u.getId());
        }

        // Keep avatar visuals as well; they may not be part of board.getAllUnits()
        currentIds.add(domainState.getPlayer(GameState.P1).getAvatar().getId());
        currentIds.add(domainState.getPlayer(GameState.P2).getAvatar().getId());

        // 2) delete visuals that no longer exist in domain state
        for (String existingId : new HashSet<>(templateState.visualUnits.keySet())) {
            if (!currentIds.contains(existingId)) {
                deleteUnitIfPresent(out, templateState, existingId);
            }
        }

        // 3) draw/update all current units
        int boardW = domainState.getRules().getBoardWidth();
        int boardH = domainState.getRules().getBoardHeight();

        for (Unit u : domainState.getBoard().getAllUnits()) {
            TilePos pos = u.getPosition();

            // Guard against null / illegal positions; otherwise visuals may appear
            // outside the board and look like "floating" sprites.
            if (pos == null || pos.x() < 0 || pos.x() >= boardW || pos.y() < 0 || pos.y() >= boardH) {
                deleteUnitIfPresent(out, templateState, u.getId());
                continue;
            }

            structures.basic.Unit visual = ensureVisualUnit(out, templateState, u);
            structures.basic.Tile tile = BasicObjectBuilders.loadTile(pos.x(), pos.y());
            BasicCommands.drawUnit(out, visual, tile);

            if (!templateState.uiInitialUnitsDrawn) {
                templateState.pendingUnitHp.put(u.getId(), u.getHp());
                templateState.pendingUnitAtk.put(u.getId(), u.getAttack());
            } else {
                BasicCommands.setUnitHealth(out, visual, u.getHp());
                BasicCommands.setUnitAttack(out, visual, u.getAttack());
            }
        }
    }

    public static void moveUnit(ActorRef out, structures.GameState templateState, Unit domainUnit) {
        if (domainUnit == null || domainUnit.getPosition() == null) return;

        TilePos pos = domainUnit.getPosition();
        if (pos.x() < 0 || pos.y() < 0) return;

        // IMPORTANT:
        // Only send a move command for units that already have a visual object on screen.
        // If the visual does not exist yet, draw it directly instead of moving it.
        structures.basic.Unit visual = templateState.visualUnits.get(domainUnit.getId());

        structures.basic.Tile tile = BasicObjectBuilders.loadTile(pos.x(), pos.y());

        if (visual == null) {
            visual = ensureVisualUnit(out, templateState, domainUnit);
            BasicCommands.drawUnit(out, visual, tile);

            if (!templateState.uiInitialUnitsDrawn) {
                templateState.pendingUnitHp.put(domainUnit.getId(), domainUnit.getHp());
                templateState.pendingUnitAtk.put(domainUnit.getId(), domainUnit.getAttack());
            } else {
                BasicCommands.setUnitHealth(out, visual, domainUnit.getHp());
                BasicCommands.setUnitAttack(out, visual, domainUnit.getAttack());
            }
            return;
        }

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

        // Domain name is "Ironcliffe Guardian", but the existing conf file is ironcliff_guardian.json
        if ("ironcliffe_guardian".equals(file)) {
            file = "ironcliff_guardian";
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

    // NEW: flush delayed unit health/attack to avoid frontend "undefined text" crash
    public static void flushPendingUnitStats(ActorRef out, structures.GameState templateState) {
        if (templateState == null) return;

        for (Map.Entry<String, Integer> e : new HashMap<>(templateState.pendingUnitAtk).entrySet()) {
            String unitId = e.getKey();
            Integer atk = e.getValue();
            if (atk == null) continue;

            structures.basic.Unit visual = templateState.visualUnits.get(unitId);
            if (visual != null) {
                BasicCommands.setUnitAttack(out, visual, atk);
                templateState.pendingUnitAtk.remove(unitId);
            }
        }

        for (Map.Entry<String, Integer> e : new HashMap<>(templateState.pendingUnitHp).entrySet()) {
            String unitId = e.getKey();
            Integer hp = e.getValue();
            if (hp == null) continue;

            structures.basic.Unit visual = templateState.visualUnits.get(unitId);
            if (visual != null) {
                BasicCommands.setUnitHealth(out, visual, hp);
                templateState.pendingUnitHp.remove(unitId);
            }
        }

        if (!templateState.uiInitialUnitsDrawn
                && templateState.pendingUnitHp.isEmpty()
                && templateState.pendingUnitAtk.isEmpty()) {
            templateState.uiInitialUnitsDrawn = true;
        }
    }
}