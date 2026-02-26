package game.card;

import game.card.effect.TeleportEffect;

public class PortalStepSpellCard extends SpellCard {
    public PortalStepSpellCard(String id, String name, int cost) {
        super(id, name, cost, TargetSpec.portalStepTarget(), new TeleportEffect());
    }
}
