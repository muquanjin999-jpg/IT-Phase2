package game.model;

import java.util.Objects;

public class Player<T> {
    private final String id;
    private final String displayName;
    private final Avatar avatar;
    private final Deck<T> deck;
    private final Hand<T> hand;
    private final DiscardPile<T> discardPile;

    private int mana;
    private int maxMana;

    private int hornOfTheForsakenRobustness = 0;
    
    public Player(String id, String displayName, Avatar avatar, Deck<T> deck, Hand<T> hand, DiscardPile<T> discardPile) {
        this.id = Objects.requireNonNull(id);
        this.displayName = Objects.requireNonNull(displayName);
        this.avatar = Objects.requireNonNull(avatar);
        this.deck = Objects.requireNonNull(deck);
        this.hand = Objects.requireNonNull(hand);
        this.discardPile = Objects.requireNonNull(discardPile);
        this.mana = 0;
        this.maxMana = 0;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Avatar getAvatar() { return avatar; }
    public Deck<T> getDeck() { return deck; }
    public Hand<T> getHand() { return hand; }
    public DiscardPile<T> getDiscardPile() { return discardPile; }

    public int getMana() { return mana; }
    public void setMana(int mana) { this.mana = Math.max(0, mana); }

    public int getMaxMana() { return maxMana; }
    public void setMaxMana(int maxMana) { this.maxMana = Math.max(0, maxMana); }

    public boolean canPay(int cost) {
        return mana >= cost;
    }

    public void spendMana(int cost) {
        if (cost < 0) throw new IllegalArgumentException("cost < 0");
        if (mana < cost) throw new IllegalStateException("Insufficient mana");
        mana -= cost;
    }
    
    public void equipHornOfTheForsaken(int robustness) {
        this.hornOfTheForsakenRobustness = Math.max(0, robustness);
    }

    public int getHornOfTheForsakenRobustness() {
        return hornOfTheForsakenRobustness;
    }

    @Override
    public String toString() {
        return "Player{" +
                "id='" + id + '\'' +
                ", mana=" + mana +
                "/" + maxMana +
                ", hand=" + hand.size() +
                ", deck=" + deck.size() +
                ", discard=" + discardPile.size() +
                '}';
    }
}
