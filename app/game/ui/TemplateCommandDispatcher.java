package game.ui;

import akka.actor.ActorRef;
import commands.BasicCommands;
import game.model.TilePos;
import game.model.Unit;
import game.model.GameState;
import game.model.Player;
import utils.BasicObjectBuilders;
import utils.StaticConfFiles;

import java.util.HashSet;
import java.util.Set;

/**
 * Minimal bridge that renders a small subset of the domain {@code game.model.GameState}
 * using the template's {@link commands.BasicCommands}.
 *
 * Notes for your Phase 2 integration:
 * - This is intentionally minimal: it renders tiles + both avatars.
 * - Once your move/attack/card pipeline is wired, you will extend this into a full
 *   CommandDispatcher (see your class list).
 */
public final class TemplateCommandDispatcher {

    private TemplateCommandDispatcher() {}

    /**
     * Draw the 9x5 grid and the two avatars.
     */
    public static void renderInitialBoardAndAvatars(ActorRef out, game.model.GameState<?> domainState) {
        // 1) Draw board tiles
        for (int x = 0; x < domainState.getRules().getBoardWidth(); x++) {
            for (int y = 0; y < domainState.getRules().getBoardHeight(); y++) {
                BasicCommands.drawTile(out, BasicObjectBuilders.loadTile(x, y), 0);
            }
        }

        // 2) Draw both avatars using the template avatar configs.
        drawAvatar(out, domainState.getPlayer(game.model.GameState.P1).getAvatar(), StaticConfFiles.humanAvatar);
        drawAvatar(out, domainState.getPlayer(game.model.GameState.P2).getAvatar(), StaticConfFiles.aiAvatar);
    }

    private static void drawAvatar(ActorRef out, Unit avatar, String avatarConf) {
        TilePos p = avatar.getPosition();
        structures.basic.Tile tile = BasicObjectBuilders.loadTile(p.x(), p.y());

        // Load a template Unit for visuals, then overwrite ATK/HP to match domain.
        structures.basic.Unit visual = BasicObjectBuilders.loadUnit(avatarConf, avatar.getId().hashCode(), structures.basic.Unit.class);
        BasicCommands.drawUnit(out, visual, tile);
        BasicCommands.setUnitHealth(out, visual, avatar.getHp());
        BasicCommands.setUnitAttack(out, visual, avatar.getAtk());
    }

    // ---------------------------------------------------------------------
    // Minimal “Phase 2” rendering helpers (units, player stats, highlighting)
    // ---------------------------------------------------------------------

    /**
     * Draw or update both players' HP/mana panels.
     */
    public static void renderPlayerStats(ActorRef out, GameState<?> domainState) {
        Player<?> p1 = domainState.getPlayer(GameState.P1);
        Player<?> p2 = domainState.getPlayer(GameState.P2);

        // Template panels are fixed as Player1 (human) and Player2 (AI)
        structures.basic.Player bp1 = new structures.basic.Player(p1.getAvatar().getHp(), p1.getMana());
        structures.basic.Player bp2 = new structures.basic.Player(p2.getAvatar().getHp(), p2.getMana());
        BasicCommands.setPlayer1Health(out, bp1);
        BasicCommands.setPlayer2Health(out, bp2);
        BasicCommands.setPlayer1Mana(out, bp1);
        BasicCommands.setPlayer2Mana(out, bp2);
    }

    /**
     * Ensure a domain unit has a corresponding visual unit instance (cached in template GameState).
     */
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

    /** Draw all units currently on the board (idempotent for already-cached units). */
    public static void renderAllUnits(ActorRef out, structures.GameState templateState, GameState<?> domainState) {
        for (Unit u : domainState.getBoard().getAllUnits()) {
            structures.basic.Unit visual = ensureVisualUnit(out, templateState, u);
            structures.basic.Tile tile = BasicObjectBuilders.loadTile(u.getPosition().x(), u.getPosition().y());
            BasicCommands.drawUnit(out, visual, tile);
            BasicCommands.setUnitHealth(out, visual, u.getHp());
            BasicCommands.setUnitAttack(out, visual, u.getAtk());
        }
    }

    /** Move an already-rendered unit. */
    public static void moveUnit(ActorRef out, structures.GameState templateState, Unit domainUnit) {
        structures.basic.Unit visual = ensureVisualUnit(out, templateState, domainUnit);
        structures.basic.Tile tile = BasicObjectBuilders.loadTile(domainUnit.getPosition().x(), domainUnit.getPosition().y());
        BasicCommands.moveUnitToTile(out, visual, tile);
    }

    /** Delete a unit if it exists visually. */
    public static void deleteUnitIfPresent(ActorRef out, structures.GameState templateState, String unitId) {
        structures.basic.Unit visual = templateState.visualUnits.remove(unitId);
        if (visual != null) {
            BasicCommands.deleteUnit(out, visual);
        }
    }

    /**
     * Highlight a set of tiles with mode=1. Clears previous highlights first.
     */
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
        // Avatars are handled elsewhere.
        String name = domainUnit.getName();
        String file = name.toLowerCase().replace(" ", "_")
                .replace("-", "_")
                .replace("'", "")
                .replace("/", "_");

        // Token shortcut
        if ("wraithling".equals(file)) {
            return StaticConfFiles.wraithling;
        }
        return "conf/gameconfs/units/" + file + ".json";
    }
}
