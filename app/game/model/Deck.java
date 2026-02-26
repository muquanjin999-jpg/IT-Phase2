package game.model;

import java.util.*;

public class Deck<T> {
    private final Deque<T> cards = new ArrayDeque<>();

    public Deck() {}

    public Deck(Collection<T> initialCards) {
        for (T c : initialCards) cards.addLast(c);
    }

    public int size() { return cards.size(); }
    public boolean isEmpty() { return cards.isEmpty(); }

    public void addBottom(T card) { cards.addLast(card); }

    public Optional<T> draw() {
        return Optional.ofNullable(cards.pollFirst());
    }

    public void shuffle(Random random) {
        List<T> tmp = new ArrayList<>(cards);
        Collections.shuffle(tmp, random);
        cards.clear();
        for (T c : tmp) cards.addLast(c);
    }

    public List<T> snapshot() {
        return new ArrayList<>(cards);
    }
}
