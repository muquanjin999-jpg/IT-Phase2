package game.card;

import game.model.Player;

public abstract class Card {
    private final String id;
    private final String name;
    private final int cost;
    private final CardType type;

    protected Card(String id, String name, int cost, CardType type) {
        this.id = id;
        this.name = name;
        this.cost = cost;
        this.type = type;
    }

    public final String getId() { return id; }
    public final String getName() { return name; }
    public final int getCost() { return cost; }
    public final CardType getType() { return type; }

    public boolean isPlayable(CardPlayContext ctx, String ownerId) {
        Player<Card> player = ctx.getState().getPlayer(ownerId);
        return ctx.getManaSystem().canPay(player, cost);
    }

    public abstract void play(CardPlayContext ctx, String ownerId, CardTarget target);

    @Override
    public String toString() {
        return name + "(" + id + ", cost=" + cost + ", " + type + ")";
    }
}
